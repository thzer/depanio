package tn.iset.gabes.helpdesk.dto.response;

import tn.iset.gabes.helpdesk.entity.enums.EquipmentType;

import java.time.LocalDate;

/**
 * Fiche d'un materiel.
 * Le champ {@code qrContent} contient l'URL encodee dans l'etiquette QR.
 */
public record EquipmentResponse(
        Long id,
        String name,
        EquipmentType type,
        String typeLabel,
        String brand,
        String model,
        String serialNumber,
        String location,
        String qrToken,
        String qrContent,
        LocalDate installedAt,
        LocalDate warrantyEnd,
        boolean underWarranty,
        boolean active,
        Long establishmentId,
        String establishmentName
) {
}
