package tn.iset.gabes.helpdesk.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import tn.iset.gabes.helpdesk.entity.enums.TicketStatus;

/**
 * Changement de statut d'un ticket.
 * <p>
 * La transition demandee est verifiee par le service a partir des
 * transitions declarees dans {@link TicketStatus} : un passage incoherent
 * est refuse.
 */
public record ChangeStatusRequest(

        @NotNull(message = "Le nouveau statut est obligatoire.")
        TicketStatus status,

        /** Obligatoire pour le statut RESOLVED : description de la solution. */
        String resolutionNote,

        /** Note de satisfaction de 1 a 5, proposee a la cloture par le client. */
        @Min(value = 1, message = "La note minimale est 1.")
        @Max(value = 5, message = "La note maximale est 5.")
        Integer satisfactionRating
) {
}
