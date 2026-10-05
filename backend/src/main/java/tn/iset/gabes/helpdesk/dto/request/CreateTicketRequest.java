package tn.iset.gabes.helpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.iset.gabes.helpdesk.entity.enums.Priority;
import tn.iset.gabes.helpdesk.entity.enums.TicketCategory;

/**
 * Declaration d'un nouveau probleme.
 * <p>
 * Le champ {@code establishmentId} est ignore lorsque l'auteur est un
 * CLIENT : son etablissement est alors deduit de son compte, afin qu'il ne
 * puisse pas creer de ticket au nom d'un autre etablissement.
 */
public record CreateTicketRequest(

        @NotBlank(message = "Le titre est obligatoire.")
        @Size(max = 150, message = "Le titre ne peut pas dépasser 150 caractères.")
        String title,

        @NotBlank(message = "La description est obligatoire.")
        @Size(min = 10, message = "La description doit contenir au moins 10 caractères.")
        String description,

        @NotNull(message = "La catégorie est obligatoire.")
        TicketCategory category,

        @NotNull(message = "La priorité est obligatoire.")
        Priority priority,

        /** Materiel concerne. Facultatif. */
        Long equipmentId,

        /** Reserve aux roles ADMIN et TECHNICIAN, qui saisissent pour un client. */
        Long establishmentId
) {
}
