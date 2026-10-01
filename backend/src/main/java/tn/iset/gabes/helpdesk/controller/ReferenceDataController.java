package tn.iset.gabes.helpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.iset.gabes.helpdesk.dto.response.EnumOption;
import tn.iset.gabes.helpdesk.entity.enums.EquipmentType;
import tn.iset.gabes.helpdesk.entity.enums.EstablishmentType;
import tn.iset.gabes.helpdesk.entity.enums.Priority;
import tn.iset.gabes.helpdesk.entity.enums.Role;
import tn.iset.gabes.helpdesk.entity.enums.TicketCategory;
import tn.iset.gabes.helpdesk.entity.enums.TicketStatus;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Listes de valeurs alimentant les menus deroulants du frontend.
 * <p>
 * Les libelles sont ainsi definis a un seul endroit, cote serveur.
 * Ajouter une categorie de panne ne demande donc aucune modification du
 * code Angular, et aucun risque de divergence entre les deux.
 */
@RestController
@RequestMapping("/api/reference")
@Tag(name = "Donnees de reference", description = "Listes de valeurs pour les formulaires")
public class ReferenceDataController {

    @GetMapping("/all")
    @Operation(summary = "Toutes les listes de valeurs en un appel",
            description = "Appele une fois au demarrage du frontend.")
    public ResponseEntity<Map<String, List<EnumOption>>> getAll() {
        return ResponseEntity.ok(Map.of(
                "priorities", getPriorities(),
                "statuses", getStatuses(),
                "categories", getCategories(),
                "equipmentTypes", getEquipmentTypes(),
                "establishmentTypes", getEstablishmentTypes(),
                "roles", getRoles()
        ));
    }

    @GetMapping("/priorities")
    @Operation(summary = "Niveaux de priorite, du plus urgent au moins urgent")
    public List<EnumOption> getPriorities() {
        return Arrays.stream(Priority.values())
                .sorted(Comparator.comparingInt(Priority::getWeight))
                .map(value -> new EnumOption(value.name(), value.getLabel()))
                .toList();
    }

    @GetMapping("/statuses")
    @Operation(summary = "Statuts du cycle de vie d'un ticket")
    public List<EnumOption> getStatuses() {
        return Arrays.stream(TicketStatus.values())
                .map(value -> new EnumOption(value.name(), value.getLabel()))
                .toList();
    }

    @GetMapping("/categories")
    @Operation(summary = "Categories de probleme")
    public List<EnumOption> getCategories() {
        return Arrays.stream(TicketCategory.values())
                .map(value -> new EnumOption(value.name(), value.getLabel()))
                .toList();
    }

    @GetMapping("/equipment-types")
    @Operation(summary = "Types de materiel")
    public List<EnumOption> getEquipmentTypes() {
        return Arrays.stream(EquipmentType.values())
                .map(value -> new EnumOption(value.name(), value.getLabel()))
                .toList();
    }

    @GetMapping("/establishment-types")
    @Operation(summary = "Types d'etablissement")
    public List<EnumOption> getEstablishmentTypes() {
        return Arrays.stream(EstablishmentType.values())
                .map(value -> new EnumOption(value.name(), value.getLabel()))
                .toList();
    }

    @GetMapping("/roles")
    @Operation(summary = "Roles utilisateurs")
    public List<EnumOption> getRoles() {
        return Arrays.stream(Role.values())
                .map(value -> new EnumOption(value.name(), value.getLabel()))
                .toList();
    }
}
