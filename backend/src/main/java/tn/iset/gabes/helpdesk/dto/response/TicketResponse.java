package tn.iset.gabes.helpdesk.dto.response;

import tn.iset.gabes.helpdesk.entity.enums.Priority;
import tn.iset.gabes.helpdesk.entity.enums.TicketCategory;
import tn.iset.gabes.helpdesk.entity.enums.TicketStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Vue detaillee d'un ticket, renvoyee par la fiche de consultation.
 * <p>
 * Le champ {@code allowedTransitions} indique au frontend les statuts
 * atteignables depuis l'etat courant : l'interface n'affiche ainsi que les
 * actions reellement autorisees, et la regle reste definie cote serveur.
 */
public record TicketResponse(
        Long id,
        String reference,
        String title,
        String description,
        TicketCategory category,
        String categoryLabel,
        Priority priority,
        String priorityLabel,
        TicketStatus status,
        String statusLabel,

        Long establishmentId,
        String establishmentName,
        String establishmentCity,
        String establishmentPhone,

        Long equipmentId,
        String equipmentName,
        String equipmentLocation,

        Long createdById,
        String createdByName,

        Long assignedToId,
        String assignedToName,

        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime assignedAt,
        LocalDateTime resolvedAt,
        LocalDateTime closedAt,
        LocalDateTime slaDueAt,

        boolean slaBreached,
        boolean overdue,
        Long hoursRemaining,
        Long resolutionTimeHours,

        String resolutionNote,
        Integer satisfactionRating,

        List<TicketStatus> allowedTransitions,
        List<CommentResponse> comments,
        List<HistoryResponse> history
) {
}
