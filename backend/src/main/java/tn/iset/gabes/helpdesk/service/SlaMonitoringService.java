package tn.iset.gabes.helpdesk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    /**
     * Point d'entree planifie. Volontairement court : il delegue aux deux
     * methodes correspondant aux deux roles decrits plus haut, chacune
     * dans sa propre transaction.
     */
    @Scheduled(cron = "${helpdesk.sla.check-cron:0 */5 * * * *}")
    public void monitorSlaDeadlines() {
        try {
            int breached = markBreachedTickets();
            int warned = sendApproachingDeadlineWarnings();
            if (breached > 0 || warned > 0) {
                log.info("Surveillance SLA : {} depassement(s) constate(s), {} alerte(s) envoyee(s)",
                        breached, warned);
            }
        } catch (Exception ex) {
            // Une erreur ici ne doit pas empecher les executions suivantes
            log.error("La surveillance des SLA a echoue : {}", ex.getMessage(), ex);
        }
    }

    /**
     * Marque les tickets dont l'echeance vient d'etre depassee.
     *
     * @return le nombre de tickets concernes
     */
    @Transactional
    public int markBreachedTickets() {
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
     *
     * @return le nombre d'alertes envoyees
     */
    @Transactional(readOnly = true)
    public int sendApproachingDeadlineWarnings() {
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
