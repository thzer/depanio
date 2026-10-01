package tn.iset.gabes.helpdesk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.iset.gabes.helpdesk.entity.Notification;

import java.time.LocalDateTime;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadFalse(Long recipientId);

    /** Marque d'un seul coup toutes les notifications non lues d'un utilisateur. */
    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.read = true, n.readAt = :now
            WHERE n.recipient.id = :recipientId AND n.read = false
            """)
    int markAllAsRead(@Param("recipientId") Long recipientId,
                      @Param("now") LocalDateTime now);
}
