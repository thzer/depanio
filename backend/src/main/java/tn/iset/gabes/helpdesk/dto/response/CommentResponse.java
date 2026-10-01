package tn.iset.gabes.helpdesk.dto.response;

import java.time.LocalDateTime;

/**
 * Message rattache a un ticket.
 */
public record CommentResponse(
        Long id,
        String content,
        boolean internalNote,
        Long authorId,
        String authorName,
        String authorRole,
        LocalDateTime createdAt
) {
}
