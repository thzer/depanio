package tn.iset.gabes.helpdesk.dto.response;

/**
 * Reponse d'une authentification reussie.
 *
 * @param accessToken  jeton a placer dans l'en-tete Authorization
 * @param refreshToken jeton permettant d'obtenir un nouveau jeton d'acces
 * @param tokenType    schema d'authentification, toujours "Bearer"
 * @param expiresIn    duree de validite du jeton d'acces, en secondes
 * @param user         profil de l'utilisateur connecte
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {

    public static AuthResponse of(String accessToken, String refreshToken,
                                  long expiresIn, UserResponse user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
