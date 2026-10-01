package tn.iset.gabes.helpdesk.common.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Format unique des reponses d'erreur de l'API.
 * <p>
 * Un format stable permet au frontend de traiter toutes les erreurs au
 * meme endroit, sans deviner la structure renvoyee selon le cas.
 *
 * @param timestamp  date et heure de l'erreur
 * @param status     code HTTP
 * @param error      libelle du code HTTP
 * @param message    message destine a l'utilisateur
 * @param path       chemin de la requete concernee
 * @param fieldErrors erreurs de validation par champ, absentes si non applicable
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(LocalDateTime.now(), status, error, message, path, null);
    }

    public static ApiError withFieldErrors(int status, String error, String message,
                                           String path, Map<String, String> fieldErrors) {
        return new ApiError(LocalDateTime.now(), status, error, message, path, fieldErrors);
    }
}
