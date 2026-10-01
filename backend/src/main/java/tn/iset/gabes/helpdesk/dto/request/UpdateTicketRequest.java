package tn.iset.gabes.helpdesk.dto.request;

import jakarta.validation.constraints.Size;
import tn.iset.gabes.helpdesk.entity.enums.Priority;
import tn.iset.gabes.helpdesk.entity.enums.TicketCategory;

/**
 * Modification d'un ticket existant.
 * Tout champ laisse a null reste inchange.
 */
public record UpdateTicketRequest(

        @Size(max = 150, message = "Le titre ne peut pas depasser 150 caracteres.")
        String title,

        String description,

        TicketCategory category,

        Priority priority,

        Long equipmentId
) {
}
