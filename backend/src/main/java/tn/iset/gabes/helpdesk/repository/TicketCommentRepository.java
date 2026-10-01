package tn.iset.gabes.helpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.iset.gabes.helpdesk.entity.TicketComment;

import java.util.List;

@Repository
public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {

    List<TicketComment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    /** Variante destinee aux utilisateurs CLIENT : les notes internes sont exclues. */
    List<TicketComment> findByTicketIdAndInternalNoteFalseOrderByCreatedAtAsc(Long ticketId);
}
