package tn.iset.gabes.helpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.iset.gabes.helpdesk.dto.request.CreateUserRequest;
import tn.iset.gabes.helpdesk.dto.request.UpdateUserRequest;
import tn.iset.gabes.helpdesk.dto.response.PageResponse;
import tn.iset.gabes.helpdesk.dto.response.TechnicianWorkloadResponse;
import tn.iset.gabes.helpdesk.dto.response.UserResponse;
import tn.iset.gabes.helpdesk.entity.enums.Role;
import tn.iset.gabes.helpdesk.service.UserService;

import java.util.List;

/**
 * Administration des comptes utilisateurs.
 * <p>
 * L'ensemble de ces operations est reserve aux administrateurs, a
 * l'exception de la liste des techniciens, dont un technicien a besoin
 * pour transferer un dossier.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs", description = "Comptes administrateurs, techniciens et clients")
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Creer un compte utilisateur",
            description = "Un compte de role CLIENT doit etre rattache a un etablissement.")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rechercher des comptes")
    public ResponseEntity<PageResponse<UserResponse>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 20, sort = "fullName") Pageable pageable) {
        return ResponseEntity.ok(userService.search(search, role, pageable));
    }

    @GetMapping("/technicians")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    @Operation(summary = "Techniciens actifs",
            description = "Destine a la liste deroulante d'affectation.")
    public ResponseEntity<List<UserResponse>> getTechnicians() {
        return ResponseEntity.ok(userService.getActiveTechnicians());
    }

    @GetMapping("/technicians/workload")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Charge de travail des techniciens",
            description = "Classement du moins charge au plus charge, pour repartir "
                    + "les affectations.")
    public ResponseEntity<List<TechnicianWorkloadResponse>> getWorkload() {
        return ResponseEntity.ok(userService.getTechnicianWorkload());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Consulter un compte")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier un compte")
    public ResponseEntity<UserResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activer ou desactiver un compte",
            description = "Un compte n'est jamais supprime afin de conserver "
                    + "l'historique de ses tickets.")
    public ResponseEntity<UserResponse> setActive(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(userService.setActive(id, active));
    }

    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reinitialiser le mot de passe d'un compte")
    public ResponseEntity<Void> resetPassword(
            @PathVariable Long id,
            @RequestParam String newPassword) {
        userService.resetPassword(id, newPassword);
        return ResponseEntity.noContent().build();
    }
}
