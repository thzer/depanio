package tn.iset.gabes.helpdesk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.gabes.helpdesk.dto.response.DashboardResponse;
import tn.iset.gabes.helpdesk.dto.response.DashboardResponse.ChartPoint;
import tn.iset.gabes.helpdesk.dto.response.TicketSummaryResponse;
import tn.iset.gabes.helpdesk.entity.enums.Priority;
import tn.iset.gabes.helpdesk.entity.enums.TicketCategory;
import tn.iset.gabes.helpdesk.entity.enums.TicketStatus;
import tn.iset.gabes.helpdesk.mapper.TicketMapper;
import tn.iset.gabes.helpdesk.repository.TicketRepository;
import tn.iset.gabes.helpdesk.security.SecurityUtils;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Calcul des indicateurs du tableau de bord.
 * <p>
 * Les agregats sont demandes a la base de donnees plutot que calcules en
 * memoire : compter cinq mille tickets par statut en SQL reste immediat,
 * alors que les charger tous en Java pour les parcourir ne tiendrait pas
 * la montee en charge.
 * <p>
 * Le perimetre est automatiquement restreint a son propre etablissement
 * lorsque l'utilisateur connecte a le role CLIENT.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int URGENT_TICKETS_LIMIT = 5;
    private static final int TREND_MONTHS = 12;
    private static final Locale FRENCH = Locale.FRENCH;

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        Long scope = SecurityUtils.getTenantFilter();

        long total = countTotal(scope);
        long open = ticketRepository.countOpen(scope);
        long breached = ticketRepository.countBreached(scope);
        long thisMonth = ticketRepository.countCreatedSince(startOfCurrentMonth(), scope);

        List<ChartPoint> byStatus = mapStatusCounts(scope);
        long resolved = byStatus.stream()
                .filter(point -> TicketStatus.RESOLVED.name().equals(point.key())
                        || TicketStatus.CLOSED.name().equals(point.key()))
                .mapToLong(ChartPoint::value)
                .sum();

        return new DashboardResponse(
                total,
                open,
                resolved,
                breached,
                thisMonth,
                ticketRepository.averageResolutionTimeInHours(scope),
                computeComplianceRate(total, breached),
                byStatus,
                mapPriorityCounts(scope),
                mapCategoryCounts(scope),
                mapTechnicianCounts(scope),
                mapMonthlyTrend(scope),
                findUrgentTickets()
        );
    }

    /**
     * Taux de respect du delai contractuel.
     *
     * @return pourcentage arrondi a une decimale, 100 si aucun ticket
     */
    private double computeComplianceRate(long total, long breached) {
        if (total == 0) {
            return 100.0;
        }
        double rate = (double) (total - breached) / total * 100.0;
        return Math.round(rate * 10.0) / 10.0;
    }

    private long countTotal(Long scope) {
        return mapStatusCounts(scope).stream().mapToLong(ChartPoint::value).sum();
    }

    private List<ChartPoint> mapStatusCounts(Long scope) {
        return ticketRepository.countGroupedByStatus(scope).stream()
                .map(row -> {
                    TicketStatus status = (TicketStatus) row[0];
                    return new ChartPoint(status.getLabel(),
                            ((Number) row[1]).longValue(), status.name());
                })
                .toList();
    }

    private List<ChartPoint> mapPriorityCounts(Long scope) {
        return ticketRepository.countOpenGroupedByPriority(scope).stream()
                .map(row -> {
                    Priority priority = (Priority) row[0];
                    return new ChartPoint(priority.getLabel(),
                            ((Number) row[1]).longValue(), priority.name());
                })
                .sorted((a, b) -> Integer.compare(
                        Priority.valueOf(a.key()).getWeight(),
                        Priority.valueOf(b.key()).getWeight()))
                .toList();
    }

    private List<ChartPoint> mapCategoryCounts(Long scope) {
        return ticketRepository.countGroupedByCategory(scope).stream()
                .map(row -> {
                    TicketCategory category = (TicketCategory) row[0];
                    return new ChartPoint(category.getLabel(),
                            ((Number) row[1]).longValue(), category.name());
                })
                .toList();
    }

    /** Charge par technicien : sans objet pour un client, qui ne la voit pas. */
    private List<ChartPoint> mapTechnicianCounts(Long scope) {
        if (scope != null) {
            return List.of();
        }
        return ticketRepository.countOpenGroupedByTechnician().stream()
                .map(row -> ChartPoint.of((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    /** Volume mensuel sur les douze derniers mois, libelles en francais. */
    private List<ChartPoint> mapMonthlyTrend(Long scope) {
        LocalDateTime from = LocalDateTime.now()
                .minusMonths(TREND_MONTHS - 1L)
                .withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        return ticketRepository.countGroupedByMonth(from, scope).stream()
                .map(row -> {
                    int year = ((Number) row[0]).intValue();
                    int month = ((Number) row[1]).intValue();
                    String label = Month.of(month).getDisplayName(TextStyle.SHORT, FRENCH)
                            + " " + year;
                    return new ChartPoint(label, ((Number) row[2]).longValue(),
                            year + "-" + String.format("%02d", month));
                })
                .toList();
    }

    /**
     * Tickets ouverts dont l'echeance contractuelle est la plus proche.
     * <p>
     * Le tri porte sur {@code slaDueAt} et non sur la priorite : comme les
     * priorites sont stockees sous forme de texte, un tri SQL les
     * classerait par ordre alphabetique, ce qui n'a aucun sens metier.
     * L'echeance, elle, integre deja la priorite puisqu'elle en est
     * calculee.
     */
    private List<TicketSummaryResponse> findUrgentTickets() {
        Long scope = SecurityUtils.getTenantFilter();
        return ticketRepository.search(
                        scope, null, null, null, true, null,
                        PageRequest.of(0, URGENT_TICKETS_LIMIT,
                                Sort.by(Sort.Direction.ASC, "slaDueAt")))
                .map(ticketMapper::toSummary)
                .getContent();
    }

    private LocalDateTime startOfCurrentMonth() {
        return LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
    }
}
