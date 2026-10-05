package tn.iset.gabes.helpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.iset.gabes.helpdesk.entity.enums.EquipmentType;

import java.time.LocalDate;

/**
 * Enregistrement d'un materiel chez un client.
 * Le jeton du QR code n'apparait pas ici : il est genere par le serveur.
 */
public record EquipmentRequest(

        @NotBlank(message = "Le nom du matériel est obligatoire.")
        @Size(max = 120, message = "Le nom ne peut pas dépasser 120 caractères.")
        String name,

        @NotNull(message = "Le type de matériel est obligatoire.")
        EquipmentType type,

        String brand,

        String model,

        String serialNumber,

        String location,

        LocalDate installedAt,

        LocalDate warrantyEnd,

        @NotNull(message = "L’établissement est obligatoire.")
        Long establishmentId,

        Boolean active
) {
}
