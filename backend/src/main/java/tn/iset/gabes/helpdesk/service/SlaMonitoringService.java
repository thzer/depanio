package tn.iset.gabes.helpdesk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tn.iset.gabes.helpdesk.entity.Ticket;
import tn.iset.gabes.helpdesk.entity.TicketHistory;
import tn.iset.gabes.helpdesk.entity.User;
import tn.iset.gabes.helpdesk.entity.enums.HistoryAction;
import tn.iset.gabes.helpdesk.entity.enums.NotificationType;
import tn.iset.gabes.helpdesk.entity.enums.Role;
import tn.iset.gabes.helpdesk.repository.TicketRepository;
import tn.iset.gabes.helpdesk.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Surveillance automatique des delais de resolution contractuels.
 * <p>
 * Cette tache s'execute periodiquement, sans intervention humaine, et
 * remplit deux roles :
 * <ol>
 *   <li><b>Prevention</b> : alerter le technicien lorsque l'echeance d'un
 *       ticket qui lui est affecte approche.</li>
 *   <li><b>Constat</b> : marquer les tickets dont l'echeance est depassee,
 *       informer les administrateurs et inscrire l'evenement dans
 *       l'historique du ticket.</li>
 * </ol>
 * La frequence est definie par la propriete
 * {@code helpdesk.sla.check-cron} et vaut cinq minutes par defaut.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlaMonitoringService {

    /** Avance avec laquelle l'alerte preventive est envoyee. */
    private static final long WARNING_WINDOW_MINUTES = 60;

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PlatformTransactionManager transactionManager;

    /**
     * Point d'entree planifie. Volontairement court : il delegue aux deux
     * methodes correspondant aux deux roles decrits plus haut, chacune
     * dans sa propre transaction.
     * <p>
     * Les transactions sont ouvertes explicitement et non par l'annotation
     * {@code @Transactional} : celle-ci serait sans effet ici, puisque les
     * deux methodes sont appelees directement depuis cette classe et non
     * via le proxy Spring qui porte le comportement transactionnel. Sans
     * transaction ouverte, le chargement differe de l'historique du ticket
     * echouerait.
     * <p>
     * Deux transactions distinctes plutot qu'une seule : l'echec du
     * constat de depassement ne doit pas empecher l'envoi des alertes
     * preventives, et reciproquement.
     */
    @Scheduled(cron = "${helpdesk.sla.check-cron:0 */5 * * * *}")
    public void monitorSlaDeadlines() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        int breached = runSafely(transaction, this::markBreachedTickets,
                "le constat des depassements");
        int warned = runSafely(transaction, this::sendApproachingDeadlineWarnings,
                "l'envoi des alertes preventives");

        if (breached > 0 || warned > 0) {
            log.info("Surveillance SLA : {} depassement(s) constate(s), {} alerte(s) envoyee(s)",
                    breached, warned);
        }
    }

    /**
     * Execute une phase dans sa propre transaction.
     * <p>
     * Une erreur est journalisee sans etre propagee : la surveillance doit
     * repasser a l'echeance suivante, meme apres un incident ponctuel.
     *
     * @return le resultat de la phase, ou 0 si elle a echoue
     */
    private int runSafely(TransactionTemplate transaction,
                          java.util.function.Supplier<Integer> phase,
                          String description) {
        try {
            Integer result = transaction.execute(status -> phase.get());
            return result != null ? result : 0;
        } catch (Exception ex) {
            log.error("La surveillance des SLA a echoue pendant {} : {}",
                    description, ex.getMessage(), ex);
            return 0;
        }
    }

    /**
     * Marque les tickets dont l'echeance vient d'etre depassee.
     * Doit etre appelee a l'interieur d'une transaction.
     *
     * @return le nombre de tickets concernes
     */
    int markBreachedTickets() {
        List<Ticket> breached = ticketRepository.findNewlyBreached(LocalDateTime.now());
        if (breached.isEmpty()) {
            return 0;
        }

        List<User> admins = userRepository.findByRoleAndActiveTrue(Role.ADMIN);

        for (Ticket ticket : breached) {
            ticket.setSlaBreached(true);
            ticket.addHistoryEntry(TicketHistory.builder()
                    .action(HistoryAction.SLA_BREACHED)
                    .newValue("Echeance du " + ticket.getSlaDueAt())
                    .comment("Depassement constate automatiquement")
                    // Aucun acteur : l'evenement est produit par le systeme
                    .build());
            ticketRepository.save(ticket);

            String message = "Delai depasse sur le ticket " + ticket.getReference()
                    + " (" + ticket.getEstablishment().getName() + ")";

            admins.forEach(admin -> notificationService.notifyUser(
                    admin, NotificationType.SLA_BREACHED, message, ticket));

            if (ticket.getAssignedTo() != null) {
                notificationService.notifyUser(ticket.getAssignedTo(),
                        NotificationType.SLA_BREACHED, message, ticket);
            }

            log.warn("SLA depasse : ticket {} echu le {}",
                    ticket.getReference(), ticket.getSlaDueAt());
        }

        return breached.size();
    }

    /**
     * Previent le technicien en charge lorsque l'echeance approche.
     * Doit etre appelee a l'interieur d'une transaction.
     *
     * @return le nombre d'alertes envoyees
     */
    int sendApproachingDeadlineWarnings() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusMinutes(WARNING_WINDOW_MINUTES);

        List<Ticket> approaching = ticketRepository.findApproachingDeadline(now, threshold);
        int sent = 0;

        for (Ticket ticket : approaching) {
            if (ticket.getAssignedTo() == null) {
                continue;
            }
            notificationService.notifyUser(ticket.getAssignedTo(),
                    NotificationType.SLA_WARNING,
                    "Le ticket " + ticket.getReference() + " doit etre resolu avant "
                            + ticket.getSlaDueAt().toLocalTime(),
                    ticket);
            sent++;
        }

        return sent;
    }
}
