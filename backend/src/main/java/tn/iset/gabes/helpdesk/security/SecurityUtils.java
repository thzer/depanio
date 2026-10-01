package tn.iset.gabes.helpdesk.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import tn.iset.gabes.helpdesk.common.exception.BusinessException;

import java.util.Optional;

/**
 * Acces a l'utilisateur authentifie depuis la couche service.
 * <p>
 * Classe utilitaire : elle evite de repeter la lecture du
 * {@code SecurityContextHolder} dans chaque service.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Classe utilitaire : instanciation interdite
    }

    public static Optional<UserPrincipal> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    /**
     * @return l'utilisateur authentifie
     * @throws BusinessException si aucun utilisateur n'est authentifie
     */
    public static UserPrincipal requireCurrentUser() {
        return getCurrentUser().orElseThrow(
                () -> new BusinessException("Aucun utilisateur authentifie."));
    }

    public static Long getCurrentUserId() {
        return requireCurrentUser().getId();
    }

    /**
     * Identifiant de l'etablissement a utiliser comme filtre de lecture.
     * <p>
     * Pour un CLIENT, renvoie son etablissement : il ne verra que ses
     * propres donnees. Pour un ADMIN ou un TECHNICIAN, renvoie null, ce qui
     * desactive le filtre et donne acces a tous les etablissements.
     */
    public static Long getTenantFilter() {
        UserPrincipal principal = requireCurrentUser();
        return principal.isClient() ? principal.getEstablishmentId() : null;
    }
}
