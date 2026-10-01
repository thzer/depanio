package tn.iset.gabes.helpdesk.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.iset.gabes.helpdesk.entity.enums.EstablishmentType;

/**
 * Creation ou modification d'un etablissement client.
 */
public record EstablishmentRequest(

        @NotBlank(message = "Le nom de l'etablissement est obligatoire.")
        @Size(max = 120, message = "Le nom ne peut pas depasser 120 caracteres.")
        String name,

        @NotNull(message = "Le type d'etablissement est obligatoire.")
        EstablishmentType type,

        String address,

        @NotBlank(message = "La ville est obligatoire.")
        String city,

        String phone,

        String contactName,

        @Email(message = "Le format de l'adresse electronique est invalide.")
        String contactEmail,

        String contractNumber,

        Boolean active
) {
}
