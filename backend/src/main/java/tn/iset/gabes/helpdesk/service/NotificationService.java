package tn.iset.gabes.helpdesk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.gabes.helpdesk.common.exception.ResourceNotFoundException;
import tn.iset.gabes.helpdesk.dto.response.NotificationResponse;
import tn.iset.gabes.helpdesk.dto.response.PageResponse;
import tn.iset.gabes.helpdesk.entity.Notification;
import tn.iset.gabes.helpdesk.entity.Ticket;
import tn.iset.gabes.helpdesk.entity.User;
import tn.iset.gabes.helpdesk.entity.enums.NotificationType;
import tn.iset.gabes.helpdesk.repository.NotificationRepository;
import tn.iset.gabes.helpdesk.security.SecurityUtils;

import java.time.LocalDateTime;

/**
 * Production et distribution des notifications.
 * <p>
 * Chaque notification suit deux chemins complementaires : elle est
 * enregistree en base, pour que l'utilisateur la retrouve a sa prochaine
 * connexion, puis poussee immediatement par WebSocket vers son navigateur
 * s'il est en ligne.
 * <p>
 * L'envoi WebSocket est protege par un bloc try/catch : l'indisponibilite
 * du canal temps reel ne doit jamais faire echouer l'operation metier qui
 * a declenche la notification.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    /** Destination personnelle, relative au prefixe /user de chaque session. */
    private static final String USER_QUEUE = "/queue/notifications";

    /** Destination publique suivie par les tableaux de bord. */
    private static final String TICKETS_TOPIC = "/topic/tickets";

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Cree une notification pour un destinataire et la pousse en temps reel.
     *
     * @param recipient destinataire
     * @param type      motif de la notification
     * @param message   texte affiche a l'utilisateur
     * @param ticket    ticket concerne, peut etre null
     */
    @Transactional
    public void notifyUser(User recipient, NotificationType type, String message, Ticket ticket) {
        if (recipient == null) {
            return;
        }

        Notification notification = notificationRepository.save(Notification.builder()
                .recipient(recipient)
                .type(type)
                .message(message)
                .ticket(ticket)
                .read(false)
                .build());

        push(recipient.getEmail(), toResponse(notification));
    }

    /**
     * Signale un evenement sur un ticket a tous les tableaux de bord
     * ouverts, sans cibler un utilisateur particulier.
     */
    public void broadcastTicketEvent(String reference, String event) {
        try {
            messagingTemplate.convertAndSend(TICKETS_TOPIC,
                    java.util.Map.of("reference", reference, "event", event));
        } catch (Exception ex) {
            log.warn("Diffusion WebSocket impossible pour le ticket {} : {}",
                    reference, ex.getMessage());
        }
    }

    private void push(String recipientEmail, NotificationResponse payload) {
        try {
            messagingTemplate.convertAndSendToUser(recipientEmail, USER_QUEUE, payload);
        } catch (Exception ex) {
            // La notification reste consultable en base : on journalise sans interrompre
            log.warn("Envoi WebSocket impossible vers {} : {}", recipientEmail, ex.getMessage());
        }
    }

    // ------------------------------------------------------------------
    //  Consultation par l'utilisateur connecte
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        Page<Notification> page =
                notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.from(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public long countUnread() {
        return notificationRepository.countByRecipientIdAndReadFalse(
                SecurityUtils.getCurrentUserId());
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        // Un utilisateur ne peut marquer comme lue qu'une notification qui lui est destinee
        if (!notification.getRecipient().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification", notificationId);
        }

        notification.markAsRead();
        notificationRepository.save(notification);
    }

    @Transactional
    public int markAllAsRead() {
        return notificationRepository.markAllAsRead(
                SecurityUtils.getCurrentUserId(), LocalDateTime.now());
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getType().getLabel(),
                notification.getMessage(),
                notification.isRead(),
                notification.getTicket() != null ? notification.getTicket().getId() : null,
                notification.getTicket() != null ? notification.getTicket().getReference() : null,
                notification.getCreatedAt()
        );
    }
}
