package tn.iset.gabes.helpdesk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.iset.gabes.helpdesk.entity.User;
import tn.iset.gabes.helpdesk.entity.enums.Role;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRoleAndActiveTrue(Role role);

    List<User> findByEstablishmentId(Long establishmentId);

    /**
     * Recherche paginee sur le nom ou l'adresse electronique, avec filtre
     * optionnel sur le role. Un parametre null desactive le critere
     * correspondant.
     */
    @Query("""
            SELECT u FROM User u
            WHERE (:search IS NULL
                   OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:role IS NULL OR u.role = :role)
            """)
    Page<User> search(@Param("search") String search,
                      @Param("role") Role role,
                      Pageable pageable);

    /**
     * Techniciens actifs classes du moins charge au plus charge.
     * Utilise pour proposer une affectation equilibree a l'administrateur.
     */
    @Query("""
            SELECT u FROM User u
            WHERE u.role = tn.iset.gabes.helpdesk.entity.enums.Role.TECHNICIAN
              AND u.active = true
            ORDER BY (SELECT COUNT(t) FROM Ticket t
                      WHERE t.assignedTo = u
                        AND t.status NOT IN (
                            tn.iset.gabes.helpdesk.entity.enums.TicketStatus.CLOSED,
                            tn.iset.gabes.helpdesk.entity.enums.TicketStatus.CANCELLED)) ASC
            """)
    List<User> findTechniciansByWorkload();

    long countByRole(Role role);
}
