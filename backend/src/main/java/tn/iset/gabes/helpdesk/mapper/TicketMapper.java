package tn.iset.gabes.helpdesk.mapper;

import org.springframework.stereotype.Component;
import tn.iset.gabes.helpdesk.dto.response.CommentResponse;
import tn.iset.gabes.helpdesk.dto.response.HistoryResponse;
import tn.iset.gabes.helpdesk.dto.response.TicketResponse;
import tn.iset.gabes.helpdesk.dto.response.TicketSummaryResponse;
import tn.iset.gabes.helpdesk.entity.Ticket;
import tn.iset.gabes.helpdesk.entity.TicketComment;
import tn.iset.gabes.helpdesk.entity.TicketHistory;
import tn.iset.gabes.helpdesk.entity.enums.TicketStatus;

import java.time.Duration;
import java.util.List;

/**
 * Conversion des tickets vers les deux formes exposees par l'API :
 * une vue allegee pour les listes et une vue detaillee pour la fiche.
 * <p>
 * Les durees sont converties en heures entieres, plus faciles a afficher
 * cote frontend qu'un objet {@code Duration} serialise.
 */
@Component
public class TicketMapper {

    /** Vue allegee, sans commentaires ni historique. */
    public TicketSummaryResponse toSummary(Ticket ticket) {
        if (ticket == null) {
            return null;
        }
        return new TicketSummaryResponse(
                ticket.getId(),
                ticket.getReference(),
                ticket.getTitle(),
                ticket.getCategory(),
                ticket.getCategory().getLabel(),
                ticket.getPriority(),
                ticket.getPriority().getLabel(),
                ticket.getStatus(),
                ticket.getStatus().getLabel(),
                ticket.getEstablishment() != null ? ticket.getEstablishment().getName() : null,
                ticket.getEquipment() != null ? ticket.getEquipment().getName() : null,
                ticket.getAssignedTo() != null ? ticket.getAssignedTo().getFullName() : null,
                ticket.getCreatedAt(),
                ticket.getSlaDueAt(),
                ticket.isSlaBreached(),
                ticket.isOverdue(),
                toHours(ticket.getTimeRemaining())
        );
    }

    /**
     * Vue detaillee.
     *
     * @param includeInternalNotes false pour un utilisateur CLIENT : les
     *                             notes internes a l'equipe technique sont
     *                             alors retirees de la reponse
     * @param allowedTransitions   statuts que l'utilisateur courant peut
     *                             reellement atteindre, calcules par le
     *                             service a partir du cycle de vie et de
     *                             son role
     */
    public TicketResponse toDetail(Ticket ticket,
                                   boolean includeInternalNotes,
                                   List<TicketStatus> allowedTransitions) {
        if (ticket == null) {
            return null;
        }

        List<CommentResponse> comments = ticket.getComments().stream()
                .filter(comment -> includeInternalNotes || !comment.isInternalNote())
                .map(this::toCommentResponse)
                .toList();

        List<HistoryResponse> history = ticket.getHistory().stream()
                .map(this::toHistoryResponse)
                .toList();

        return new TicketResponse(
                ticket.getId(),
                ticket.getReference(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory(),
                ticket.getCategory().getLabel(),
                ticket.getPriority(),
                ticket.getPriority().getLabel(),
                ticket.getStatus(),
                ticket.getStatus().getLabel(),

                ticket.getEstablishment() != null ? ticket.getEstablishment().getId() : null,
                ticket.getEstablishment() != null ? ticket.getEstablishment().getName() : null,
                ticket.getEstablishment() != null ? ticket.getEstablishment().getCity() : null,
                ticket.getEstablishment() != null ? ticket.getEstablishment().getPhone() : null,

                ticket.getEquipment() != null ? ticket.getEquipment().getId() : null,
                ticket.getEquipment() != null ? ticket.getEquipment().getName() : null,
                ticket.getEquipment() != null ? ticket.getEquipment().getLocation() : null,

                ticket.getCreatedBy() != null ? ticket.getCreatedBy().getId() : null,
                ticket.getCreatedBy() != null ? ticket.getCreatedBy().getFullName() : null,

                ticket.getAssignedTo() != null ? ticket.getAssignedTo().getId() : null,
                ticket.getAssignedTo() != null ? ticket.getAssignedTo().getFullName() : null,

                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getAssignedAt(),
                ticket.getResolvedAt(),
                ticket.getClosedAt(),
                ticket.getSlaDueAt(),

                ticket.isSlaBreached(),
                ticket.isOverdue(),
                toHours(ticket.getTimeRemaining()),
                toHours(ticket.getResolutionTime()),

                ticket.getResolutionNote(),
                ticket.getSatisfactionRating(),

                allowedTransitions,
                comments,
                history
        );
    }

    public CommentResponse toCommentResponse(TicketComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.isInternalNote(),
                comment.getAuthor() != null ? comment.getAuthor().getId() : null,
                comment.getAuthor() != null ? comment.getAuthor().getFullName() : null,
                comment.getAuthor() != null ? comment.getAuthor().getRole().getLabel() : null,
                comment.getCreatedAt()
        );
    }

    public HistoryResponse toHistoryResponse(TicketHistory entry) {
        return new HistoryResponse(
                entry.getId(),
                entry.getAction(),
                entry.getAction().getLabel(),
                entry.getOldValue(),
                entry.getNewValue(),
                entry.getComment(),
                entry.getActor() != null ? entry.getActor().getFullName() : "Systeme",
                entry.getCreatedAt()
        );
    }

    /** Convertit une duree en heures entieres, en conservant le signe. */
    private Long toHours(Duration duration) {
        return duration == null ? null : duration.toHours();
    }
}
