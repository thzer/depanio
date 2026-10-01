package tn.iset.gabes.helpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Changement de mot de passe par l'utilisateur lui-meme.
 * L'ancien mot de passe est exige afin qu'un jeton vole ne suffise pas
 * a prendre le controle definitif du compte.
 */
public record ChangePasswordRequest(

        @NotBlank(message = "Le mot de passe actuel est obligatoire.")
        String currentPassword,

        @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caracteres.")
        String newPassword
) {
}
