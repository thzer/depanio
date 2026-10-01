package tn.iset.gabes.helpdesk.dto.response;

import tn.iset.gabes.helpdesk.entity.enums.HistoryAction;

import java.time.LocalDateTime;

/**
 * Entree de la piste d'audit d'un ticket.
 */
public record HistoryResponse(
        Long id,
        HistoryAction action,
        String actionLabel,
        String oldValue,
        String newValue,
        String comment,
        String actorName,
        LocalDateTime createdAt
) {
}
