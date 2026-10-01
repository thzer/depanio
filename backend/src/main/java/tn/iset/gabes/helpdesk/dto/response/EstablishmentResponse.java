package tn.iset.gabes.helpdesk.dto.response;

import tn.iset.gabes.helpdesk.entity.enums.EstablishmentType;

import java.time.LocalDateTime;

/**
 * Fiche d'un etablissement client, enrichie de quelques indicateurs
 * calcules pour eviter au frontend des appels supplementaires.
 */
public record EstablishmentResponse(
        Long id,
        String name,
        EstablishmentType type,
        String typeLabel,
        String address,
        String city,
        String phone,
        String contactName,
        String contactEmail,
        String contractNumber,
        boolean active,
        int equipmentCount,
        long openTicketCount,
        LocalDateTime createdAt
) {
}
