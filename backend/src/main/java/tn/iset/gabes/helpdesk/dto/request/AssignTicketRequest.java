package tn.iset.gabes.helpdesk.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Affectation d'un ticket a un technicien.
 */
public record AssignTicketRequest(

        @NotNull(message = "L'identifiant du technicien est obligatoire.")
        Long technicianId
) {
}
