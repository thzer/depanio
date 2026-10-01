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
import tn.iset.gabes.helpdesk.dto.request.EstablishmentRequest;
import tn.iset.gabes.helpdesk.dto.response.EstablishmentResponse;
import tn.iset.gabes.helpdesk.dto.response.PageResponse;
import tn.iset.gabes.helpdesk.entity.enums.EstablishmentType;
import tn.iset.gabes.helpdesk.service.EstablishmentService;

import java.util.List;

/**
 * Gestion du portefeuille de clients.
 * La creation et la modification sont reservees aux administrateurs.
 */
@RestController
@RequestMapping("/api/establishments")
@RequiredArgsConstructor
@Tag(name = "Etablissements", description = "Cafes et restaurants sous contrat")
public class EstablishmentController {

    private final EstablishmentService establishmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enregistrer un nouvel etablissement client")
    public ResponseEntity<EstablishmentResponse> create(
            @Valid @RequestBody EstablishmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(establishmentService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    @Operation(summary = "Rechercher des etablissements")
    public ResponseEntity<PageResponse<EstablishmentResponse>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EstablishmentType type,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(
                establishmentService.search(search, type, active, pageable));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    @Operation(summary = "Etablissements actifs",
            description = "Destine aux listes deroulantes des formulaires.")
    public ResponseEntity<List<EstablishmentResponse>> getActive() {
        return ResponseEntity.ok(establishmentService.getActive());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un etablissement",
            description = "Un compte client ne peut consulter que le sien.")
    public ResponseEntity<EstablishmentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(establishmentService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier un etablissement")
    public ResponseEntity<EstablishmentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EstablishmentRequest request) {
        return ResponseEntity.ok(establishmentService.update(id, request));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activer ou desactiver un etablissement",
            description = "La desactivation est refusee s'il reste des tickets ouverts.")
    public ResponseEntity<EstablishmentResponse> setActive(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(establishmentService.setActive(id, active));
    }
}
