package tn.iset.gabes.helpdesk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.iset.gabes.helpdesk.entity.DemoRequest;

import java.time.LocalDateTime;

@Repository
public interface DemoRequestRepository extends JpaRepository<DemoRequest, Long> {

    Page<DemoRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<DemoRequest> findByHandledFalseOrderByCreatedAtDesc(Pageable pageable);

    long countByHandledFalse();

    /**
     * Nombre de demandes deja recues depuis une adresse donnee sur une
     * periode. Sert a ecarter les envois repetes : le formulaire est
     * public, rien n empeche de le soumettre en boucle.
     */
    long countByEmailIgnoreCaseAndCreatedAtAfter(String email, LocalDateTime since);

    /** Nombre total de demandes recues recemment, toutes adresses confondues. */
    long countByCreatedAtAfter(LocalDateTime since);
}
