package tn.iset.gabes.helpdesk.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gestionnaire global des exceptions.
 * <p>
 * Toutes les erreurs remontees par les controleurs sont converties ici en
 * reponses {@link ApiError}. Les controleurs n'ont donc aucun bloc
 * try/catch a porter, et le frontend recoit toujours la meme structure.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex,
                                                   HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, "Not Found", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex,
                                                   HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, "Bad Request", ex.getMessage(), request.getRequestURI()));
    }

    /**
     * Echecs de validation des annotations Bean Validation sur les objets
     * de requete. Les messages sont regroupes par nom de champ pour que le
     * frontend puisse les afficher sous chaque zone de saisie.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.withFieldErrors(400, "Bad Request",
                        "Les donnees envoyees sont invalides.",
                        request.getRequestURI(), fieldErrors));
    }

    /**
     * Adresse inconnue cote serveur.
     * <p>
     * Sans ce traitement, l'absence d'une ressource remonterait jusqu'au
     * filet de securite et serait signalee comme une erreur interne. Une
     * adresse qui n'existe pas n'est pas une panne : la confondre avec une
     * vraie erreur noierait les incidents reels dans les journaux.
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiError> handleUnknownPath(Exception ex,
                                                      HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, "Not Found",
                        "Cette adresse n'existe pas.", request.getRequestURI()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex,
                                                         HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(401, "Unauthorized",
                        "Authentification requise ou identifiants invalides.",
                        request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex,
                                                       HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, "Forbidden",
                        "Vous n'avez pas les droits necessaires pour cette operation.",
                        request.getRequestURI()));
    }

    /**
     * Filet de securite : toute exception non prevue est journalisee avec
     * sa pile d'appels, mais le client ne recoit qu'un message generique
     * afin de ne pas exposer le fonctionnement interne du systeme.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex,
                                                     HttpServletRequest request) {
        log.error("Erreur non geree sur {} : {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(500, "Internal Server Error",
                        "Une erreur interne est survenue. Veuillez contacter l'administrateur.",
                        request.getRequestURI()));
    }
}
