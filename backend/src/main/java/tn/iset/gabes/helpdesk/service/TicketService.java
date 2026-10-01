package tn.iset.gabes.helpdesk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.gabes.helpdesk.common.exception.BusinessException;
import tn.iset.gabes.helpdesk.common.exception.ResourceNotFoundException;
import tn.iset.gabes.helpdesk.config.HelpdeskProperties;
import tn.iset.gabes.helpdesk.dto.request.AddCommentRequest;
import tn.iset.gabes.helpdesk.dto.request.AssignTicketRequest;
import tn.iset.gabes.helpdesk.dto.request.ChangeStatusRequest;
import tn.iset.gabes.helpdesk.dto.request.CreateTicketRequest;
import tn.iset.gabes.helpdesk.dto.request.UpdateTicketRequest;
import tn.iset.gabes.helpdesk.dto.response.CommentResponse;
import tn.iset.gabes.helpdesk.dto.response.PageResponse;
import tn.iset.gabes.helpdesk.dto.response.TicketResponse;
import tn.iset.gabes.helpdesk.dto.response.TicketSummaryResponse;
import tn.iset.gabes.helpdesk.entity.Equipment;
import tn.iset.gabes.helpdesk.entity.Establishment;
import tn.iset.gabes.helpdesk.entity.Ticket;
import tn.iset.gabes.helpdesk.entity.TicketComment;
import tn.iset.gabes.helpdesk.entity.TicketHistory;
import tn.iset.gabes.helpdesk.entity.User;
import tn.iset.gabes.helpdesk.entity.enums.HistoryAction;
import tn.iset.gabes.helpdesk.entity.enums.NotificationType;
import tn.iset.gabes.helpdesk.entity.enums.Priority;
import tn.iset.gabes.helpdesk.entity.enums.Role;
import tn.iset.gabes.helpdesk.entity.enums.TicketStatus;
import tn.iset.gabes.helpdesk.mapper.TicketMapper;
import tn.iset.gabes.helpdesk.repository.EquipmentRepository;
import tn.iset.gabes.helpdesk.repository.EstablishmentRepository;
import tn.iset.gabes.helpdesk.repository.TicketRepository;
import tn.iset.gabes.helpdesk.repository.UserRepository;
import tn.iset.gabes.helpdesk.security.SecurityUtils;
import tn.iset.gabes.helpdesk.security.UserPrincipal;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

/**
 * Logique metier des tickets : creation, affectation, suivi du cycle de
 * vie et echanges.
 * <p>
 * Deux regles sont appliquees systematiquement dans cette classe plutot
 * que dans les controleurs, afin qu'aucun point d'entree ne puisse les
 * contourner :
 * <ol>
 *   <li><b>Cloisonnement</b> : un utilisateur CLIENT ne voit et ne modifie
 *       que les tickets de son propre etablissement.</li>
 *   <li><b>Coherence du cycle de vie</b> : seules les transitions de
 *       statut declarees dans {@link TicketStatus} sont acceptees.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private static final String REFERENCE_PREFIX = "TK";

    private final TicketRepository ticketRepository;
    private final EstablishmentRepository establishmentRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final TicketMapper ticketMapper;
    private final NotificationService notificationService;
    private final HelpdeskProperties properties;

    // ==================================================================
    //  Creation
    // ==================================================================

    /**
     * Enregistre un nouveau ticket.
     * <p>
     * L'etablissement retenu depend du role de l'auteur : impose a partir
     * de son compte pour un CLIENT, choisi dans la requete pour un ADMIN
     * ou un TECHNICIAN qui saisit un appel telephonique.
     */
    @Transactional
    public TicketResponse create(CreateTicketRequest request) {
        UserPrincipal principal = SecurityUtils.requireCurrentUser();
        User author = loadUser(principal.getId());

        Establishment establishment = resolveEstablishment(request, principal);
        if (!establishment.isActive()) {
            throw new BusinessException(
                    "L'etablissement " + establishment.getName() + " est desactive.");
        }

        Equipment equipment = resolveEquipment(request.equipmentId(), establishment);

        Ticket ticket = Ticket.builder()
                .reference(generateReference())
                .title(request.title().trim())
                .description(request.description().trim())
                .category(request.category())
                .priority(request.priority())
                .status(TicketStatus.NEW)
                .establishment(establishment)
                .equipment(equipment)
                .createdBy(author)
                .slaDueAt(computeSlaDeadline(request.priority()))
                .build();

        ticket.addHistoryEntry(TicketHistory.builder()
                .action(HistoryAction.CREATED)
                .newValue(TicketStatus.NEW.getLabel())
                .actor(author)
                .build());

        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket {} cree par {} pour {}",
                saved.getReference(), author.getEmail(), establishment.getName());

        notifyAdminsOfNewTicket(saved);
        notificationService.broadcastTicketEvent(saved.getReference(), "CREATED");

        return toDetail(saved, principal);
    }

    /**
     * Creation depuis une etiquette QR : le materiel et l'etablissement
     * sont deduits du jeton scanne.
     */
    @Transactional
    public TicketResponse createFromQrToken(String qrToken, CreateTicketRequest request) {
        Equipment equipment = equipmentRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun materiel ne correspond a cette etiquette QR."));

        UserPrincipal principal = SecurityUtils.requireCurrentUser();

        // Un client ne peut declarer une panne que sur le materiel de son etablissement
        if (principal.isClient()
                && !equipment.getEstablishment().getId().equals(principal.getEstablishmentId())) {
            throw new AccessDeniedException(
                    "Ce materiel n'appartient pas a votre etablissement.");
        }

        CreateTicketRequest enriched = new CreateTicketRequest(
                request.title(),
                request.description(),
                request.category(),
                request.priority(),
                equipment.getId(),
                equipment.getEstablishment().getId()
        );
        return create(enriched);
    }

    // ==================================================================
    //  Consultation
    // ==================================================================

    @Transactional(readOnly = true)
    public TicketResponse getById(Long id) {
        UserPrincipal principal = SecurityUtils.requireCurrentUser();
        Ticket ticket = loadTicket(id);
        assertCanRead(ticket, principal);
        return toDetail(ticket, principal);
    }

    @Transactional(readOnly = true)
    public TicketResponse getByReference(String reference) {
        UserPrincipal principal = SecurityUtils.requireCurrentUser();
        Ticket ticket = ticketRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun ticket ne porte la reference " + reference));
        assertCanRead(ticket, principal);
        return toDetail(ticket, principal);
    }

    /**
     * Recherche paginee.
     * <p>
     * Pour un CLIENT, le filtre d'etablissement est impose a partir de son
     * compte : la valeur eventuellement transmise dans la requete est
     * ignoree, ce qui empeche la consultation des tickets d'un tiers.
     */
    @Transactional(readOnly = true)
    public PageResponse<TicketSummaryResponse> search(Long establishmentId,
                                                      Long assignedToId,
                                                      TicketStatus status,
                                                      Priority priority,
                                                      boolean onlyOpen,
                                                      String search,
                                                      Pageable pageable) {
        UserPrincipal principal = SecurityUtils.requireCurrentUser();

        Long effectiveEstablishmentId = principal.isClient()
                ? principal.getEstablishmentId()
                : establishmentId;

        Page<Ticket> page = ticketRepository.search(
                effectiveEstablishmentId,
                assignedToId,
                status,
                priority,
                onlyOpen,
                normalize(search),
                pageable);

        return PageResponse.from(page, ticketMapper::toSummary);
    }

    /** Tickets affectes au technicien connecte. */
    @Transactional(readOnly = true)
    public PageResponse<TicketSummaryResponse> getMyAssignedTickets(boolean onlyOpen,
                                                                    Pageable pageable) {
        UserPrincipal principal = SecurityUtils.requireCurrentUser();
        Page<Ticket> page = ticketRepository.search(
                null, principal.getId(), null, null, onlyOpen, null, pageable);
        return PageResponse.from(page, ticketMapper::toSummary);
    }

    /** File d'attente des tickets non encore affectes. */
    @Transactional(readOnly = true)
    public List<TicketSummaryResponse> getUnassigned() {
        return ticketRepository.findUnassigned().stream()
                .map(ticketMapper::toSummary)
                .toList();
    }

    // ==================================================================
    //  Modification
    // ==================================================================

    @Transactional
    public TicketResponse update(Long id, UpdateTicketRequest request) {
        UserPrincipal principal = SecurityUtils.requireCurrentUser();
        User actor = loadUser(principal.getId());
        Ticket ticket = loadTicket(id);
        assertCanWrite(ticket, principal);

        if (ticket.getStatus().isFinalState()) {
            throw new BusinessException(
                    "Un ticket " + ticket.getStatus().getLabel().toLowerCase()
                            + " ne peut plus etre modifie.");
        }

        if (request.title() != null && !request.title().isBlank()) {
            ticket.setTitle(request.title().trim());
        }
        if (request.description() != null && !request.description().isBlank()) {
            ticket.setDescription(request.description().trim());
        }
        if (request.category() != null) {
            ticket.setCategory(request.category());
        }
        if (request.priority() != null && request.priority() != ticket.getPriority()) {
            Priority previous = ticket.getPriority();
            ticket.setPriority(request.priority());
            // L'echeance contractuelle est recalculee a partir de la date de creation
            ticket.setSlaDueAt(computeSlaDeadline(request.priority(), ticket.getCreatedAt()));
            ticket.addHistoryEntry(TicketHistory.builder()
                    .action(HistoryAction.PRIORITY_CHANGED)
                    .oldValue(previous.getLabel())
                    .newValue(request.priority().getLabel())
                    .actor(actor)
                    .build());
        }
        if (request.equipmentId() != null) {
            ticket.setEquipment(resolveEquipment(request.equipmentId(), ticket.getEstablishment()));
        }

        Ticket saved = ticketRepository.save(ticket);
        return toDetail(saved, principal);
    }

    /**
     * Affecte le ticket a un technicien.
     * <p>
     * Reserve aux administrateurs, et aux techniciens qui prennent en
     * charge un ticket libre.
     */
    @Transactional
    public TicketResponse assign(Long id, AssignTicketRequest request) {
        UserPrincipal principal = SecurityUtils.requireCurrentUser();
        User actor = loadUser(principal.getId());
        Ticket ticket = loadTicket(id);

        if (principal.isClient()) {
            throw new AccessDeniedException(
                    "L'affectation d'un ticket est reservee a l'equipe technique.");
        }
        if (ticket.getStatus().isFinalState()) {
            throw new BusinessException("Un ticket clos ne peut plus etre affecte.");
        }

        User technician = loadUser(request.technicianId());
        if (technician.getRole() != Role.TECHNICIAN) {
            throw new BusinessException(
                    "Seul un compte de role Technicien peut recevoir un ticket.");
        }
        if (!technician.isActive()) {
            throw new BusinessException("Ce technicien est desactive.");
        }

        // Un technicien ne peut s'attribuer qu'un ticket libre, pas reprendre celui d'un collegue
        boolean reassigning = ticket.getAssignedTo() != null
                && !ticket.getAssignedTo().getId().equals(technician.getId());
        if (principal.isTechnician() && reassigning) {
            throw new AccessDeniedException(
                    "Seul un administrateur peut reaffecter un ticket deja pris en charge.");
        }

        String previous = ticket.getAssignedTo() != null
                ? ticket.getAssignedTo().getFullName()
                : null;

        ticket.setAssignedTo(technician);
        ticket.setAssignedAt(LocalDateTime.now());
        if (ticket.getStatus() == TicketStatus.NEW) {
            ticket.setStatus(TicketStatus.ASSIGNED);
        }

        ticket.addHistoryEntry(TicketHistory.builder()
                .action(HistoryAction.ASSIGNED)
                .oldValue(previous)
                .newValue(technician.getFullName())
                .actor(actor)
                .build());

        Ticket saved = ticketRepository.save(ticket);

        notificationService.notifyUser(technician, NotificationType.TICKET_ASSIGNED,
                "Le ticket " + saved.getReference() + " vous a ete affecte : " + saved.getTitle(),
                saved);
        notificationService.broadcastTicketEvent(saved.getReference(), "ASSIGNED");

        return toDetail(saved, principal);
    }

    /**
     * Fait evoluer le statut du ticket.
     * <p>
     * La transition est validee contre les transitions autorisees, puis les
     * horodatages et les notifications correspondants sont produits.
     */
    @Transactional
    public TicketResponse changeStatus(Long id, ChangeStatusRequest request) {
        UserPrincipal principal = SecurityUtils.requireCurrentUser();
        User actor = loadUser(principal.getId());
        Ticket ticket = loadTicket(id);
        assertCanWrite(ticket, principal);

        TicketStatus current = ticket.getStatus();
        TicketStatus target = request.status();

        if (current == target) {
            throw new BusinessException(
                    "Le ticket est deja au statut " + target.getLabel() + ".");
        }
        if (!current.canMoveTo(target)) {
            throw new BusinessException(
                    "Transition interdite : un ticket " + current.getLabel().toLowerCase()
                            + " ne peut pas passer a " + target.getLabel().toLowerCase() + ".");
        }

        applyRoleRulesForTransition(principal, ticket, target);
        applyStatusSideEffects(ticket, target, request, actor);

        ticket.setStatus(target);
        ticket.addHistoryEntry(TicketHistory.builder()
                .action(HistoryAction.STATUS_CHANGED)
                .oldValue(current.getLabel())
                .newValue(target.getLabel())
                .actor(actor)
                .build());

        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket {} : {} -> {} par {}",
                saved.getReference(), current, target, actor.getEmail());

        notifyStatusChange(saved, target);
        notificationService.broadcastTicketEvent(saved.getReference(), target.name());

        return toDetail(saved, principal);
    }

    /**
     * Le role de l'utilisateur lui permet-il cette transition ?
     * <p>
     * Cette regle est utilisee a deux endroits : pour refuser une
     * transition demandee, et pour calculer la liste des actions proposees
     * au frontend. Les deux restent ainsi necessairement coherents.
     *
     * @return true si la transition est permise a cet utilisateur
     */
    private boolean isTransitionAllowedForRole(UserPrincipal principal,
                                               Ticket ticket,
                                               TicketStatus target) {
        if (principal.isAdmin()) {
            return true;
        }

        if (principal.isClient()) {
            // Le client confirme la cloture ou annule sa propre demande ;
            // le suivi technique ne lui appartient pas
            return target == TicketStatus.CLOSED || target == TicketStatus.CANCELLED;
        }

        // Technicien : uniquement sur les tickets qui lui sont affectes
        return ticket.getAssignedTo() != null
                && ticket.getAssignedTo().getId().equals(principal.getId());
    }

    /**
     * Verifie que le role de l'utilisateur autorise la transition demandee,
     * et leve une exception explicite dans le cas contraire.
     */
    private void applyRoleRulesForTransition(UserPrincipal principal,
                                             Ticket ticket,
                                             TicketStatus target) {
        if (isTransitionAllowedForRole(principal, ticket, target)) {
            return;
        }

        if (principal.isClient()) {
            throw new AccessDeniedException(
                    "Le suivi technique du ticket est assure par l'equipe de maintenance.");
        }
        throw new AccessDeniedException("Ce ticket ne vous est pas affecte.");
    }

    /**
     * Statuts que l'utilisateur courant peut reellement atteindre.
     * <p>
     * Le cycle de vie definit ce qui est coherent, le role definit ce qui
     * est permis. Le frontend recoit l'intersection des deux et n'affiche
     * donc que des actions qui aboutiront.
     */
    private List<TicketStatus> allowedTransitionsFor(Ticket ticket, UserPrincipal principal) {
        return ticket.getStatus().allowedTransitions().stream()
                .filter(target -> isTransitionAllowedForRole(principal, ticket, target))
                .sorted()
                .toList();
    }

    /** Raccourci : vue detaillee adaptee a l'utilisateur courant. */
    private TicketResponse toDetail(Ticket ticket, UserPrincipal principal) {
        return ticketMapper.toDetail(
                ticket,
                !principal.isClient(),
                allowedTransitionsFor(ticket, principal));
    }

    /**
     * Renseigne les champs lies au nouveau statut : note de resolution,
     * horodatages, note de satisfaction.
     */
    private void applyStatusSideEffects(Ticket ticket,
                                        TicketStatus target,
                                        ChangeStatusRequest request,
                                        User actor) {
        switch (target) {
            case RESOLVED -> {
                if (request.resolutionNote() == null || request.resolutionNote().isBlank()) {
                    throw new BusinessException(
                            "La description de la solution appliquee est obligatoire "
                                    + "pour resoudre un ticket.");
                }
                ticket.setResolutionNote(request.resolutionNote().trim());
                ticket.markResolvedNow();
            }
            case CLOSED -> {
                ticket.setClosedAt(LocalDateTime.now());
                if (ticket.getResolvedAt() == null) {
                    ticket.markResolvedNow();
                }
                if (request.satisfactionRating() != null) {
                    ticket.setSatisfactionRating(request.satisfactionRating());
                }
            }
            case CANCELLED -> {
                ticket.setClosedAt(LocalDateTime.now());
                if (request.resolutionNote() != null && !request.resolutionNote().isBlank()) {
                    ticket.setResolutionNote(request.resolutionNote().trim());
                }
            }
            case IN_PROGRESS -> {
                // Reouverture apres une resolution jugee incomplete
                if (ticket.getStatus() == TicketStatus.RESOLVED) {
                    ticket.clearResolution();
                    ticket.addHistoryEntry(TicketHistory.builder()
                            .action(HistoryAction.REOPENED)
                            .comment("Resolution jugee incomplete")
                            .actor(actor)
                            .build());
                }
            }
            default -> {
                // ASSIGNED et WAITING_CLIENT ne necessitent aucun traitement particulier
            }
        }
    }

    // ==================================================================
    //  Commentaires
    // ==================================================================

    @Transactional
    public CommentResponse addComment(Long ticketId, AddCommentRequest request) {
        UserPrincipal principal = SecurityUtils.requireCurrentUser();
        User author = loadUser(principal.getId());
        Ticket ticket = loadTicket(ticketId);
        assertCanRead(ticket, principal);

        // Une note interne ne peut etre redigee que par l'equipe technique
        boolean internal = request.internalNote() && !principal.isClient();

        TicketComment comment = TicketComment.builder()
                .content(request.content().trim())
                .internalNote(internal)
                .author(author)
                .build();
        ticket.addComment(comment);

        ticket.addHistoryEntry(TicketHistory.builder()
                .action(HistoryAction.COMMENT_ADDED)
                .actor(author)
                .comment(internal ? "Note interne" : null)
                .build());

        Ticket saved = ticketRepository.save(ticket);
        TicketComment persisted = saved.getComments().get(saved.getComments().size() - 1);

        if (!internal) {
            notifyCommentRecipients(saved, author);
        }

        return ticketMapper.toCommentResponse(persisted);
    }

    // ==================================================================
    //  Notifications
    // ==================================================================

    private void notifyAdminsOfNewTicket(Ticket ticket) {
        String message = "Nouveau ticket " + ticket.getReference() + " ("
                + ticket.getPriority().getLabel() + ") chez "
                + ticket.getEstablishment().getName();
        userRepository.findByRoleAndActiveTrue(Role.ADMIN).forEach(admin ->
                notificationService.notifyUser(
                        admin, NotificationType.TICKET_CREATED, message, ticket));
    }

    private void notifyStatusChange(Ticket ticket, TicketStatus target) {
        NotificationType type = switch (target) {
            case RESOLVED -> NotificationType.TICKET_RESOLVED;
            case CLOSED -> NotificationType.TICKET_CLOSED;
            default -> NotificationType.TICKET_UPDATED;
        };
        String message = "Le ticket " + ticket.getReference() + " est maintenant "
                + target.getLabel().toLowerCase() + ".";

        // L'auteur du ticket est informe de toute evolution
        notificationService.notifyUser(ticket.getCreatedBy(), type, message, ticket);

        // Le technicien en charge aussi, sauf s'il est lui-meme a l'origine du changement
        User assignee = ticket.getAssignedTo();
        if (assignee != null && !assignee.getId().equals(SecurityUtils.getCurrentUserId())) {
            notificationService.notifyUser(assignee, type, message, ticket);
        }
    }

    private void notifyCommentRecipients(Ticket ticket, User author) {
        String message = author.getFullName() + " a commente le ticket "
                + ticket.getReference();

        if (!ticket.getCreatedBy().getId().equals(author.getId())) {
            notificationService.notifyUser(
                    ticket.getCreatedBy(), NotificationType.NEW_COMMENT, message, ticket);
        }
        User assignee = ticket.getAssignedTo();
        if (assignee != null && !assignee.getId().equals(author.getId())) {
            notificationService.notifyUser(
                    assignee, NotificationType.NEW_COMMENT, message, ticket);
        }
    }

    // ==================================================================
    //  Regles d'acces
    // ==================================================================

    /** Un CLIENT ne peut lire que les tickets de son etablissement. */
    private void assertCanRead(Ticket ticket, UserPrincipal principal) {
        if (principal.isClient()
                && !ticket.getEstablishment().getId().equals(principal.getEstablishmentId())) {
            // Message volontairement identique a celui d'un ticket inexistant :
            // il ne doit pas reveler qu'un ticket portant cet identifiant existe
            throw new ResourceNotFoundException("Ticket", ticket.getId());
        }
    }

    private void assertCanWrite(Ticket ticket, UserPrincipal principal) {
        assertCanRead(ticket, principal);
    }

    // ==================================================================
    //  Utilitaires internes
    // ==================================================================

    /**
     * Produit la reference lisible du ticket, au format TK-2026-0001.
     * <p>
     * Le compteur repart a 1 chaque annee. La derniere reference de
     * l'annee en cours est relue en base a chaque creation, ce qui evite de
     * maintenir un compteur separe susceptible de se desynchroniser.
     */
    private String generateReference() {
        String prefix = REFERENCE_PREFIX + "-" + Year.now().getValue() + "-";
        int next = ticketRepository.findLastReferenceForPrefix(prefix)
                .map(last -> {
                    try {
                        return Integer.parseInt(last.substring(prefix.length())) + 1;
                    } catch (NumberFormatException ex) {
                        log.warn("Reference illisible en base : {}", last);
                        return 1;
                    }
                })
                .orElse(1);
        return prefix + String.format("%04d", next);
    }

    /** Echeance contractuelle calculee a partir de l'instant present. */
    private LocalDateTime computeSlaDeadline(Priority priority) {
        return computeSlaDeadline(priority, LocalDateTime.now());
    }

    /**
     * Echeance contractuelle calculee a partir d'une date de depart.
     * Les durees proviennent de la configuration et non du code, afin de
     * pouvoir les ajuster par contrat sans nouvelle livraison.
     */
    private LocalDateTime computeSlaDeadline(Priority priority, LocalDateTime from) {
        LocalDateTime start = from != null ? from : LocalDateTime.now();
        HelpdeskProperties.Sla sla = properties.getSla();
        int hours = switch (priority) {
            case CRITICAL -> sla.getCritical();
            case HIGH -> sla.getHigh();
            case MEDIUM -> sla.getMedium();
            case LOW -> sla.getLow();
        };
        return start.plusHours(hours);
    }

    private Establishment resolveEstablishment(CreateTicketRequest request,
                                               UserPrincipal principal) {
        if (principal.isClient()) {
            Long establishmentId = principal.getEstablishmentId();
            if (establishmentId == null) {
                throw new BusinessException(
                        "Votre compte n'est rattache a aucun etablissement. "
                                + "Contactez votre administrateur.");
            }
            return loadEstablishment(establishmentId);
        }

        if (request.establishmentId() == null) {
            throw new BusinessException(
                    "L'etablissement concerne doit etre precise.");
        }
        return loadEstablishment(request.establishmentId());
    }

    /**
     * Charge le materiel en verifiant qu'il appartient bien a
     * l'etablissement du ticket.
     */
    private Equipment resolveEquipment(Long equipmentId, Establishment establishment) {
        if (equipmentId == null) {
            return null;
        }
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Materiel", equipmentId));

        if (!equipment.getEstablishment().getId().equals(establishment.getId())) {
            throw new BusinessException(
                    "Le materiel selectionne n'appartient pas a l'etablissement "
                            + establishment.getName() + ".");
        }
        return equipment;
    }

    private Ticket loadTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
    }

    private User loadUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
    }

    private Establishment loadEstablishment(Long id) {
        return establishmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Etablissement", id));
    }

    /** Transforme une chaine vide en null pour neutraliser le filtre correspondant. */
    private String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
