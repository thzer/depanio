package tn.iset.gabes.helpdesk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.iset.gabes.helpdesk.entity.enums.NotificationType;

import java.time.LocalDateTime;

/**
 * Notification destinee a un utilisateur.
 * <p>
 * Elle est enregistree en base pour que l'utilisateur la retrouve apres
 * reconnexion, puis poussee immediatement vers son navigateur par
 * WebSocket s'il est connecte.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "notifications",
        indexes = @Index(name = "idx_notification_recipient", columnList = "recipient_id, is_read")
)
public class Notification extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String message;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    /** Ticket concerne, pour permettre la navigation directe depuis la notification. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    /** Marque la notification comme lue et horodate la lecture. */
    public void markAsRead() {
        if (!read) {
            this.read = true;
            this.readAt = LocalDateTime.now();
        }
    }
}
