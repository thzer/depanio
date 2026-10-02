package tn.iset.gabes.helpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.iset.gabes.helpdesk.dto.request.ChangePasswordRequest;
import tn.iset.gabes.helpdesk.dto.request.LoginRequest;
import tn.iset.gabes.helpdesk.dto.request.RefreshTokenRequest;
import tn.iset.gabes.helpdesk.dto.response.AuthResponse;
import tn.iset.gabes.helpdesk.dto.response.UserResponse;
import tn.iset.gabes.helpdesk.service.AuthService;

/**
 * Authentification et gestion du mot de passe de l'utilisateur connecte.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Connexion et gestion du mot de passe")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Se connecter",
            description = "Verifie les identifiants et renvoie un jeton d'acces "
                    + "ainsi qu'un jeton de renouvellement. Les echecs repetes "
                    + "suspendent temporairement les tentatives.")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, origine(httpRequest)));
    }

    /**
     * Adresse reseau de l'appelant.
     * <p>
     * L'application est servie derriere un repartiteur de charge :
     * l'adresse directe est celle de ce dernier, identique pour tout le
     * monde. La veritable origine figure dans l'en-tete transmis par le
     * repartiteur, dont seul le premier element est fiable, les suivants
     * pouvant etre forges par l'appelant.
     */
    private String origine(HttpServletRequest request) {
        String transmis = request.getHeader("X-Forwarded-For");
        if (transmis != null && !transmis.isBlank()) {
            return transmis.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Renouveler le jeton d'acces",
            description = "Delivre un nouveau jeton d'acces a partir d'un jeton "
                    + "de renouvellement encore valide.")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Profil de l'utilisateur connecte")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUserProfile());
    }

    @PostMapping("/change-password")
    @Operation(summary = "Changer son mot de passe",
            description = "Exige le mot de passe actuel afin qu'un jeton derobe "
                    + "ne suffise pas a s'approprier le compte.")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.noContent().build();
    }
}
