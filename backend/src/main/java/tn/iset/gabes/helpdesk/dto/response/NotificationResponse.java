package tn.iset.gabes.helpdesk.dto.response;

import tn.iset.gabes.helpdesk.entity.enums.NotificationType;

import java.time.LocalDateTime;

/**
 * Notification destinee a l'utilisateur connecte.
 */
public record NotificationResponse(
        Long id,
        NotificationType type,
        String typeLabel,
        String message,
        boolean read,
        Long ticketId,
        String ticketReference,
        LocalDateTime createdAt
) {
}
