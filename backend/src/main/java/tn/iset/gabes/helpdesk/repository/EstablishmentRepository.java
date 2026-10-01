package tn.iset.gabes.helpdesk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.iset.gabes.helpdesk.entity.Establishment;
import tn.iset.gabes.helpdesk.entity.enums.EstablishmentType;

import java.util.List;

@Repository
public interface EstablishmentRepository extends JpaRepository<Establishment, Long> {

    List<Establishment> findByActiveTrue();

    boolean existsByContractNumber(String contractNumber);

    /**
     * Recherche paginee par nom ou par ville, avec filtre optionnel sur le
     * type d'etablissement. Un parametre null desactive le critere.
     */
    @Query("""
            SELECT e FROM Establishment e
            WHERE (:search IS NULL
                   OR LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(e.city) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:type IS NULL OR e.type = :type)
              AND (:active IS NULL OR e.active = :active)
            """)
    Page<Establishment> search(@Param("search") String search,
                               @Param("type") EstablishmentType type,
                               @Param("active") Boolean active,
                               Pageable pageable);

    long countByActiveTrue();
}
