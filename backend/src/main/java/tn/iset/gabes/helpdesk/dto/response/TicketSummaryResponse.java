package tn.iset.gabes.helpdesk.dto.response;

import tn.iset.gabes.helpdesk.entity.enums.Priority;
import tn.iset.gabes.helpdesk.entity.enums.TicketCategory;
import tn.iset.gabes.helpdesk.entity.enums.TicketStatus;

import java.time.LocalDateTime;

/**
 * Version allegee d'un ticket, destinee aux listes et au tableau de bord.
 * <p>
 * Les commentaires et l'historique sont volontairement absents : les
 * charger pour chaque ligne d'une liste de cent tickets provoquerait
 * autant de requetes inutiles.
 */
public record TicketSummaryResponse(
        Long id,
        String reference,
        String title,
        TicketCategory category,
        String categoryLabel,
        Priority priority,
        String priorityLabel,
        TicketStatus status,
        String statusLabel,
        String establishmentName,
        String equipmentName,
        String assignedToName,
        LocalDateTime createdAt,
        LocalDateTime slaDueAt,
        boolean slaBreached,
        boolean overdue,
        /** Heures restantes avant l'echeance, negatif si depassee, null si sans objet. */
        Long hoursRemaining
) {
}
