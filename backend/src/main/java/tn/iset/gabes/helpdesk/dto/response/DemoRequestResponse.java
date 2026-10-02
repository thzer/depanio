package tn.iset.gabes.helpdesk.dto.response;

import java.time.LocalDateTime;

/**
 * Demande de demonstration, telle que l administrateur la consulte.
 */
public record DemoRequestResponse(
        Long id,
        String contactName,
        String email,
        String phone,
        String companyName,
        String establishmentCount,
        String message,
        boolean handled,
        LocalDateTime handledAt,
        LocalDateTime createdAt
) {
}
