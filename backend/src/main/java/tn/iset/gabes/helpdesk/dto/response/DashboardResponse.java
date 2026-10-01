package tn.iset.gabes.helpdesk.dto.response;

import java.util.List;

/**
 * Donnees agregees du tableau de bord.
 * <p>
 * Un seul appel alimente toute la page : indicateurs, repartitions et
 * courbe mensuelle. Cela evite une dizaine d'appels paralleles au
 * chargement, et garantit que tous les chiffres affiches correspondent au
 * meme instant de lecture.
 */
public record DashboardResponse(

        // ----- Indicateurs principaux -----
        long totalTickets,
        long openTickets,
        long resolvedTickets,
        long breachedTickets,
        long ticketsThisMonth,

        /** Delai moyen de resolution, en heures. Null si aucun ticket resolu. */
        Double averageResolutionHours,

        /** Pourcentage de tickets traites dans le delai contractuel. */
        double slaComplianceRate,

        // ----- Repartitions -----
        List<ChartPoint> byStatus,
        List<ChartPoint> byPriority,
        List<ChartPoint> byCategory,
        List<ChartPoint> byTechnician,

        /** Volume mensuel sur les douze derniers mois. */
        List<ChartPoint> monthlyTrend,

        /** Tickets ouverts les plus urgents, pour action immediate. */
        List<TicketSummaryResponse> urgentTickets
) {

    /**
     * Point d'un graphique : un libelle et une valeur.
     *
     * @param label libelle affiche sur l'axe ou dans la legende
     * @param value effectif associe
     * @param key   valeur technique, utile pour appliquer une couleur fixe
     */
    public record ChartPoint(String label, long value, String key) {

        public static ChartPoint of(String label, long value) {
            return new ChartPoint(label, value, label);
        }
    }
}
