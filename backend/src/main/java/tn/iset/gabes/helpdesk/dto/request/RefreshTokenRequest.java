package tn.iset.gabes.helpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Demande de renouvellement du jeton d'acces.
 */
public record RefreshTokenRequest(

        @NotBlank(message = "Le jeton de renouvellement est obligatoire.")
        String refreshToken
) {
}
