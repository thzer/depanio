package tn.iset.gabes.helpdesk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.iset.gabes.helpdesk.dto.response.DashboardResponse;
import tn.iset.gabes.helpdesk.service.DashboardService;

/**
 * Indicateurs agreges du tableau de bord.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Tableau de bord", description = "Indicateurs et statistiques")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Donnees du tableau de bord",
            description = "Un seul appel renvoie les indicateurs, les repartitions "
                    + "et la courbe mensuelle. Le perimetre est limite a son propre "
                    + "etablissement pour un compte client.")
    public ResponseEntity<DashboardResponse> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboard());
    }
}
