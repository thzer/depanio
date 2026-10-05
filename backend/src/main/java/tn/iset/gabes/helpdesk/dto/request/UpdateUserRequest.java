package tn.iset.gabes.helpdesk.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Modification d'un compte existant.
 * Le mot de passe n'est pas modifiable ici : il dispose de son propre
 * point d'entree afin d'exiger l'ancien mot de passe.
 */
public record UpdateUserRequest(

        @Size(max = 120, message = "Le nom ne peut pas dépasser 120 caractères.")
        String fullName,

        @Email(message = "Le format de l’adresse électronique est invalide.")
        String email,

        String phone,

        String speciality,

        Boolean active
) {
}
