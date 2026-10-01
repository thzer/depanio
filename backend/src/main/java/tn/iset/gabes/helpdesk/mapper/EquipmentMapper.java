package tn.iset.gabes.helpdesk.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.iset.gabes.helpdesk.config.HelpdeskProperties;
import tn.iset.gabes.helpdesk.dto.request.EquipmentRequest;
import tn.iset.gabes.helpdesk.dto.response.EquipmentResponse;
import tn.iset.gabes.helpdesk.entity.Equipment;

/**
 * Conversion des materiels.
 * <p>
 * Le champ {@code qrContent} est l'URL encodee dans l'etiquette collee sur
 * la machine. Elle est reconstruite ici a partir de l'URL du frontend
 * configuree, et non stockee en base : si l'application change de domaine,
 * les etiquettes existantes restent valables sans migration de donnees.
 */
@Component
@RequiredArgsConstructor
public class EquipmentMapper {

    private final HelpdeskProperties properties;

    public EquipmentResponse toResponse(Equipment equipment) {
        if (equipment == null) {
            return null;
        }
        return new EquipmentResponse(
                equipment.getId(),
                equipment.getName(),
                equipment.getType(),
                equipment.getType().getLabel(),
                equipment.getBrand(),
                equipment.getModel(),
                equipment.getSerialNumber(),
                equipment.getLocation(),
                equipment.getQrToken(),
                buildQrContent(equipment.getQrToken()),
                equipment.getInstalledAt(),
                equipment.getWarrantyEnd(),
                equipment.isUnderWarranty(),
                equipment.isActive(),
                equipment.getEstablishment() != null ? equipment.getEstablishment().getId() : null,
                equipment.getEstablishment() != null ? equipment.getEstablishment().getName() : null
        );
    }

    /**
     * URL ouverte par le telephone apres lecture de l'etiquette QR.
     */
    public String buildQrContent(String qrToken) {
        return properties.getFrontendUrl() + "/scan/" + qrToken;
    }

    /** Applique les champs modifiables d'une requete sur une entite. */
    public void apply(EquipmentRequest request, Equipment target) {
        target.setName(request.name());
        target.setType(request.type());
        target.setBrand(request.brand());
        target.setModel(request.model());
        target.setSerialNumber(request.serialNumber());
        target.setLocation(request.location());
        target.setInstalledAt(request.installedAt());
        target.setWarrantyEnd(request.warrantyEnd());
        if (request.active() != null) {
            target.setActive(request.active());
        }
    }
}
