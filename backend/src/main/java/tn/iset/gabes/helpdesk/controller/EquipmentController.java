package tn.iset.gabes.helpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import tn.iset.gabes.helpdesk.dto.request.EquipmentRequest;
import tn.iset.gabes.helpdesk.dto.response.EquipmentResponse;
import tn.iset.gabes.helpdesk.dto.response.PageResponse;
import tn.iset.gabes.helpdesk.entity.enums.EquipmentType;
import tn.iset.gabes.helpdesk.service.EquipmentService;

import java.util.List;

/**
 * Gestion du parc de materiel et des etiquettes QR.
 */
@RestController
@RequestMapping("/api/equipments")
@RequiredArgsConstructor
@Tag(name = "Materiel", description = "Parc installe chez les clients et etiquettes QR")
public class EquipmentController {

    private final EquipmentService equipmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enregistrer un materiel",
            description = "Le jeton de l'etiquette QR est genere automatiquement.")
    public ResponseEntity<EquipmentResponse> create(
            @Valid @RequestBody EquipmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipmentService.create(request));
    }

    @GetMapping
    @Operation(summary = "Rechercher du materiel",
            description = "Un compte client ne voit que son propre parc.")
    public ResponseEntity<PageResponse<EquipmentResponse>> search(
            @RequestParam(required = false) Long establishmentId,
            @RequestParam(required = false) EquipmentType type,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(
                equipmentService.search(establishmentId, type, search, pageable));
    }

    @GetMapping("/by-establishment/{establishmentId}")
    @Operation(summary = "Materiel actif d'un etablissement",
            description = "Destine a la liste deroulante du formulaire de ticket.")
    public ResponseEntity<List<EquipmentResponse>> getByEstablishment(
            @PathVariable Long establishmentId) {
        return ResponseEntity.ok(equipmentService.getByEstablishment(establishmentId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un materiel")
    public ResponseEntity<EquipmentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(equipmentService.getById(id));
    }

    @GetMapping("/by-qr/{qrToken}")
    @Operation(summary = "Identifier un materiel a partir de son etiquette QR",
            description = "Appele par le frontend apres la lecture du code, pour "
                    + "pre-remplir le formulaire de declaration de panne.")
    public ResponseEntity<EquipmentResponse> getByQrToken(@PathVariable String qrToken) {
        return ResponseEntity.ok(equipmentService.getByQrToken(qrToken));
    }

    @GetMapping(value = "/{id}/qr-code", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Telecharger l'etiquette QR en PNG",
            description = "Image prete a imprimer et a coller sur la machine.")
    public ResponseEntity<byte[]> downloadQrCode(
            @PathVariable Long id,
            @RequestParam(defaultValue = "300") int size) {

        byte[] image = equipmentService.generateQrCodeImage(id, size);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename("qr-materiel-" + id + ".png")
                                .build().toString())
                .body(image);
    }

    @GetMapping("/most-problematic")
    @Operation(summary = "Materiels les plus souvent en panne",
            description = "Classement utile pour conseiller un remplacement.")
    public ResponseEntity<List<EquipmentService.MostProblematicEquipment>> getMostProblematic(
            @RequestParam(required = false) Long establishmentId,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(
                equipmentService.getMostProblematic(establishmentId, limit));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier un materiel")
    public ResponseEntity<EquipmentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EquipmentRequest request) {
        return ResponseEntity.ok(equipmentService.update(id, request));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mettre un materiel hors service ou le remettre en service")
    public ResponseEntity<EquipmentResponse> setActive(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(equipmentService.setActive(id, active));
    }
}
