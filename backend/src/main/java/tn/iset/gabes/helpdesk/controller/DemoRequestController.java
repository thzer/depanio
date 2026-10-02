package tn.iset.gabes.helpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.iset.gabes.helpdesk.dto.request.DemoRequestForm;
import tn.iset.gabes.helpdesk.dto.response.DemoRequestResponse;
import tn.iset.gabes.helpdesk.dto.response.PageResponse;
import tn.iset.gabes.helpdesk.service.DemoRequestService;

import java.util.Map;

/**
 * Demandes de demonstration.
 * <p>
 * Le depot est ouvert a tous : il provient de la page commerciale, ou le
 * visiteur n'a par definition pas de compte. La consultation, elle, est
 * reservee aux administrateurs.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Demandes de demonstration", description = "Prospects issus de la page commerciale")
public class DemoRequestController {

    private final DemoRequestService demoRequestService;

    /**
     * Depot d'une demande depuis la page commerciale.
     * <p>
     * Place sous {@code /api/public} : ce prefixe est le seul ouvert sans
     * authentification, ce qui rend la surface publique de l'API visible
     * d'un coup d'oeil dans la configuration de securite.
     */
    @PostMapping("/api/public/demo-requests")
    @SecurityRequirements
    @Operation(summary = "Demander une demonstration",
            description = "Enregistre la demande et previent les administrateurs.")
    public ResponseEntity<Map<String, String>> submit(@Valid @RequestBody DemoRequestForm form) {
        demoRequestService.submit(form);
        return ResponseEntity.ok(Map.of(
                "message", "Votre demande est bien enregistree. Nous vous repondons sous 24 heures."));
    }

    @GetMapping("/api/demo-requests")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Consulter les demandes recues")
    public ResponseEntity<PageResponse<DemoRequestResponse>> list(
            @RequestParam(defaultValue = "false") boolean onlyPending,
            @PageableDefault(size = 30) Pageable pageable) {
        return ResponseEntity.ok(demoRequestService.list(onlyPending, pageable));
    }

    @GetMapping("/api/demo-requests/pending-count")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Nombre de demandes en attente de reponse")
    public ResponseEntity<Map<String, Long>> countPending() {
        return ResponseEntity.ok(Map.of("count", demoRequestService.countPending()));
    }

    @PatchMapping("/api/demo-requests/{id}/handled")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Marquer une demande comme traitee")
    public ResponseEntity<DemoRequestResponse> markHandled(@PathVariable Long id) {
        return ResponseEntity.ok(demoRequestService.markHandled(id));
    }
}
