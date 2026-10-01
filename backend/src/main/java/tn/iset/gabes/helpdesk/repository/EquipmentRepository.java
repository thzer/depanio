package tn.iset.gabes.helpdesk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.iset.gabes.helpdesk.entity.Equipment;
import tn.iset.gabes.helpdesk.entity.enums.EquipmentType;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    /** Resolution de l'etiquette QR collee sur la machine. */
    Optional<Equipment> findByQrToken(String qrToken);

    List<Equipment> findByEstablishmentIdAndActiveTrue(Long establishmentId);

    List<Equipment> findByEstablishmentId(Long establishmentId);

    @Query("""
            SELECT e FROM Equipment e
            WHERE (:establishmentId IS NULL OR e.establishment.id = :establishmentId)
              AND (:type IS NULL OR e.type = :type)
              AND (:search IS NULL
                   OR LOWER(e.name)         LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(e.serialNumber) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Equipment> search(@Param("establishmentId") Long establishmentId,
                           @Param("type") EquipmentType type,
                           @Param("search") String search,
                           Pageable pageable);

    /**
     * Materiel le plus souvent en panne : utile pour conseiller un client
     * sur un remplacement. Renvoie des lignes [equipement, nombre de tickets].
     */
    @Query("""
            SELECT t.equipment, COUNT(t) FROM Ticket t
            WHERE t.equipment IS NOT NULL
              AND (:establishmentId IS NULL OR t.establishment.id = :establishmentId)
            GROUP BY t.equipment
            ORDER BY COUNT(t) DESC
            """)
    List<Object[]> findMostProblematic(@Param("establishmentId") Long establishmentId,
                                       Pageable pageable);
}
