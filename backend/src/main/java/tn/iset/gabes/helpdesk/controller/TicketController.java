package tn.iset.gabes.helpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import tn.iset.gabes.helpdesk.dto.request.AddCommentRequest;
import tn.iset.gabes.helpdesk.dto.request.AssignTicketRequest;
import tn.iset.gabes.helpdesk.dto.request.ChangeStatusRequest;
import tn.iset.gabes.helpdesk.dto.request.CreateTicketRequest;
import tn.iset.gabes.helpdesk.dto.request.UpdateTicketRequest;
import tn.iset.gabes.helpdesk.dto.response.CommentResponse;
import tn.iset.gabes.helpdesk.dto.response.PageResponse;
import tn.iset.gabes.helpdesk.dto.response.TicketResponse;
import tn.iset.gabes.helpdesk.dto.response.TicketSummaryResponse;
import tn.iset.gabes.helpdesk.entity.enums.Priority;
import tn.iset.gabes.helpdesk.entity.enums.TicketStatus;
import tn.iset.gabes.helpdesk.service.TicketService;

import java.util.List;

/**
 * Points d'entree relatifs aux tickets.
 * <p>
 * Les controles d'acces fins restent dans la couche service, qui connait
 * l'etat du ticket. Les annotations {@code @PreAuthorize} presentes ici ne
 * filtrent que ce qui depend du seul role.
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "Declaration et suivi des demandes d'intervention")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @Operation(summary = "Declarer un nouveau probleme")
    public ResponseEntity<TicketResponse> create(
            @Valid @RequestBody CreateTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.create(request));
    }

    @PostMapping("/from-qr/{qrToken}")
    @Operation(summary = "Declarer un probleme depuis une etiquette QR",
            description = "Le materiel et l'etablissement sont deduits du jeton scanne.")
    public ResponseEntity<TicketResponse> createFromQr(
            @PathVariable String qrToken,
            @Valid @RequestBody CreateTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.createFromQrToken(qrToken, request));
    }

    @GetMapping
    @Operation(summary = "Rechercher des tickets",
            description = "Tous les filtres sont facultatifs. Pour un compte client, "
                    + "le perimetre est automatiquement limite a son etablissement.")
    public ResponseEntity<PageResponse<TicketSummaryResponse>> search(
            @Parameter(description = "Filtrer sur un etablissement")
            @RequestParam(required = false) Long establishmentId,

            @Parameter(description = "Filtrer sur le technicien en charge")
            @RequestParam(required = false) Long assignedToId,

            @RequestParam(required = false) TicketStatus status,

            @RequestParam(required = false) Priority priority,

            @Parameter(description = "Exclure les tickets clotures et annules")
            @RequestParam(defaultValue = "false") boolean onlyOpen,

            @Parameter(description = "Recherche sur la reference ou le titre")
            @RequestParam(required = false) String search,

            @PageableDefault(size = 20, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(ticketService.search(
                establishmentId, assignedToId, status, priority, onlyOpen, search, pageable));
    }

    @GetMapping("/my-assigned")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    @Operation(summary = "Tickets affectes au technicien connecte")
    public ResponseEntity<PageResponse<TicketSummaryResponse>> getMyAssigned(
            @RequestParam(defaultValue = "true") boolean onlyOpen,
            @PageableDefault(size = 20, sort = "slaDueAt") Pageable pageable) {
        return ResponseEntity.ok(ticketService.getMyAssignedTickets(onlyOpen, pageable));
    }

    @GetMapping("/unassigned")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    @Operation(summary = "File d'attente des tickets non affectes")
    public ResponseEntity<List<TicketSummaryResponse>> getUnassigned() {
        return ResponseEntity.ok(ticketService.getUnassigned());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un ticket")
    public ResponseEntity<TicketResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getById(id));
    }

    @GetMapping("/by-reference/{reference}")
    @Operation(summary = "Consulter un ticket par sa reference",
            description = "Par exemple TK-2026-0001.")
    public ResponseEntity<TicketResponse> getByReference(@PathVariable String reference) {
        return ResponseEntity.ok(ticketService.getByReference(reference));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un ticket",
            description = "Les champs laisses vides restent inchanges.")
    public ResponseEntity<TicketResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketRequest request) {
        return ResponseEntity.ok(ticketService.update(id, request));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    @Operation(summary = "Affecter le ticket a un technicien")
    public ResponseEntity<TicketResponse> assign(
            @PathVariable Long id,
            @Valid @RequestBody AssignTicketRequest request) {
        return ResponseEntity.ok(ticketService.assign(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Changer le statut du ticket",
            description = "Seules les transitions autorisees par le cycle de vie "
                    + "sont acceptees. Les statuts accessibles depuis l'etat courant "
                    + "sont indiques par le champ allowedTransitions de la fiche.")
    public ResponseEntity<TicketResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request) {
        return ResponseEntity.ok(ticketService.changeStatus(id, request));
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Ajouter un message au ticket",
            description = "Le drapeau internalNote est ignore pour un compte client.")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody AddCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.addComment(id, request));
    }
}
