package tn.iset.gabes.helpdesk.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Identifiants soumis lors de la connexion.
 */
public record LoginRequest(

        @NotBlank(message = "L’adresse électronique est obligatoire.")
        @Email(message = "Le format de l’adresse électronique est invalide.")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        String password
) {
}
