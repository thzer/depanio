package tn.iset.gabes.helpdesk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.iset.gabes.helpdesk.entity.Ticket;
import tn.iset.gabes.helpdesk.entity.enums.Priority;
import tn.iset.gabes.helpdesk.entity.enums.TicketStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Acces aux tickets.
 * <p>
 * Les methodes de recherche acceptent des criteres facultatifs : un
 * parametre null desactive le filtre correspondant. Cela permet de servir
 * toutes les vues de l'application avec une seule requete, au lieu de
 * multiplier les methodes pour chaque combinaison de filtres.
 * <p>
 * Le parametre {@code establishmentId} assure le cloisonnement
 * multi-locataire : la couche service le force a la valeur de
 * l'etablissement de l'utilisateur lorsque celui-ci a le role CLIENT.
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByReference(String reference);

    /**
     * Derniere reference attribuee pour une annee donnee, utilisee pour
     * calculer le numero sequentiel suivant.
     *
     * @param prefix prefixe de l'annee, par exemple "TK-2026-"
     */
    @Query("SELECT MAX(t.reference) FROM Ticket t WHERE t.reference LIKE CONCAT(:prefix, '%')")
    Optional<String> findLastReferenceForPrefix(@Param("prefix") String prefix);

    /**
     * Recherche principale, utilisee par la liste des tickets et par les
     * exports. Tous les criteres sont facultatifs.
     */
    @Query("""
            SELECT t FROM Ticket t
            WHERE (:establishmentId IS NULL OR t.establishment.id = :establishmentId)
              AND (:assignedToId IS NULL OR t.assignedTo.id = :assignedToId)
              AND (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:onlyOpen = false OR t.status NOT IN (
                        tn.iset.gabes.helpdesk.entity.enums.TicketStatus.CLOSED,
                        tn.iset.gabes.helpdesk.entity.enums.TicketStatus.CANCELLED))
              AND (:search IS NULL
                   OR LOWER(t.reference) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(t.title)     LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Ticket> search(@Param("establishmentId") Long establishmentId,
                        @Param("assignedToId") Long assignedToId,
                        @Param("status") TicketStatus status,
                        @Param("priority") Priority priority,
                        @Param("onlyOpen") boolean onlyOpen,
                        @Param("search") String search,
                        Pageable pageable);

    /** Tickets non affectes, du plus urgent au plus ancien. */
    @Query("""
            SELECT t FROM Ticket t
            WHERE t.assignedTo IS NULL
              AND t.status = tn.iset.gabes.helpdesk.entity.enums.TicketStatus.NEW
            ORDER BY t.priority ASC, t.createdAt ASC
            """)
    List<Ticket> findUnassigned();

    /**
     * Tickets encore ouverts dont l'echeance contractuelle est depassee et
     * qui n'ont pas encore ete marques comme tels. Consomme par la tache
     * planifiee de surveillance des SLA.
     */
    @Query("""
            SELECT t FROM Ticket t
            WHERE t.slaBreached = false
              AND t.slaDueAt IS NOT NULL
              AND t.slaDueAt < :now
              AND t.status NOT IN (
                    tn.iset.gabes.helpdesk.entity.enums.TicketStatus.CLOSED,
                    tn.iset.gabes.helpdesk.entity.enums.TicketStatus.CANCELLED,
                    tn.iset.gabes.helpdesk.entity.enums.TicketStatus.RESOLVED)
            """)
    List<Ticket> findNewlyBreached(@Param("now") LocalDateTime now);

    /**
     * Tickets ouverts dont l'echeance approche, pour envoyer une alerte
     * preventive au technicien avant le depassement.
     */
    @Query("""
            SELECT t FROM Ticket t
            WHERE t.slaBreached = false
              AND t.slaDueAt BETWEEN :now AND :threshold
              AND t.status NOT IN (
                    tn.iset.gabes.helpdesk.entity.enums.TicketStatus.CLOSED,
                    tn.iset.gabes.helpdesk.entity.enums.TicketStatus.CANCELLED,
                    tn.iset.gabes.helpdesk.entity.enums.TicketStatus.RESOLVED)
            """)
    List<Ticket> findApproachingDeadline(@Param("now") LocalDateTime now,
                                         @Param("threshold") LocalDateTime threshold);

    // ------------------------------------------------------------------
    //  Requetes d'agregation alimentant le tableau de bord
    //  Chaque ligne renvoyee est un tableau [valeur groupee, effectif].
    // ------------------------------------------------------------------

    @Query("""
            SELECT t.status, COUNT(t) FROM Ticket t
            WHERE (:establishmentId IS NULL OR t.establishment.id = :establishmentId)
            GROUP BY t.status
            """)
    List<Object[]> countGroupedByStatus(@Param("establishmentId") Long establishmentId);

    @Query("""
            SELECT t.priority, COUNT(t) FROM Ticket t
            WHERE (:establishmentId IS NULL OR t.establishment.id = :establishmentId)
              AND t.status NOT IN (
                    tn.iset.gabes.helpdesk.entity.enums.TicketStatus.CLOSED,
                    tn.iset.gabes.helpdesk.entity.enums.TicketStatus.CANCELLED)
            GROUP BY t.priority
            """)
    List<Object[]> countOpenGroupedByPriority(@Param("establishmentId") Long establishmentId);

    @Query("""
            SELECT t.category, COUNT(t) FROM Ticket t
            WHERE (:establishmentId IS NULL OR t.establishment.id = :establishmentId)
            GROUP BY t.category
            ORDER BY COUNT(t) DESC
            """)
    List<Object[]> countGroupedByCategory(@Param("establishmentId") Long establishmentId);

    /** Volume mensuel sur une periode : [annee, mois, effectif]. */
    @Query("""
            SELECT YEAR(t.createdAt), MONTH(t.createdAt), COUNT(t) FROM Ticket t
            WHERE t.createdAt >= :from
              AND (:establishmentId IS NULL OR t.establishment.id = :establishmentId)
            GROUP BY YEAR(t.createdAt), MONTH(t.createdAt)
            ORDER BY YEAR(t.createdAt), MONTH(t.createdAt)
            """)
    List<Object[]> countGroupedByMonth(@Param("from") LocalDateTime from,
                                       @Param("establishmentId") Long establishmentId);

    /** Charge de travail par technicien : [nom du technicien, effectif ouvert]. */
    @Query("""
            SELECT t.assignedTo.fullName, COUNT(t) FROM Ticket t
            WHERE t.assignedTo IS NOT NULL
              AND t.status NOT IN (
                    tn.iset.gabes.helpdesk.entity.enums.TicketStatus.CLOSED,
                    tn.iset.gabes.helpdesk.entity.enums.TicketStatus.CANCELLED)
            GROUP BY t.assignedTo.id, t.assignedTo.fullName
            ORDER BY COUNT(t) DESC
            """)
    List<Object[]> countOpenGroupedByTechnician();

    /**
     * Delai moyen de resolution, exprime en heures.
     * TIMESTAMPDIFF est une fonction du moteur MySQL / MariaDB appelee ici
     * via la syntaxe generique de Hibernate.
     */
    @Query(value = """
            SELECT AVG(TIMESTAMPDIFF(MINUTE, t.created_at, t.resolved_at)) / 60.0
            FROM tickets t
            WHERE t.resolved_at IS NOT NULL
              AND (:establishmentId IS NULL OR t.establishment_id = :establishmentId)
            """, nativeQuery = true)
    Double averageResolutionTimeInHours(@Param("establishmentId") Long establishmentId);

    long countByStatus(TicketStatus status);

    long countBySlaBreachedTrue();

    @Query("""
            SELECT COUNT(t) FROM Ticket t
            WHERE t.status NOT IN (
                    tn.iset.gabes.helpdesk.entity.enums.TicketStatus.CLOSED,
                    tn.iset.gabes.helpdesk.entity.enums.TicketStatus.CANCELLED)
              AND (:establishmentId IS NULL OR t.establishment.id = :establishmentId)
            """)
    long countOpen(@Param("establishmentId") Long establishmentId);

    @Query("""
            SELECT COUNT(t) FROM Ticket t
            WHERE t.createdAt >= :from
              AND (:establishmentId IS NULL OR t.establishment.id = :establishmentId)
            """)
    long countCreatedSince(@Param("from") LocalDateTime from,
                           @Param("establishmentId") Long establishmentId);

    @Query("""
            SELECT COUNT(t) FROM Ticket t
            WHERE t.slaBreached = true
              AND (:establishmentId IS NULL OR t.establishment.id = :establishmentId)
            """)
    long countBreached(@Param("establishmentId") Long establishmentId);

    long countByAssignedToIdAndStatusNotIn(Long technicianId, List<TicketStatus> excluded);
}
