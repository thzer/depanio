package tn.iset.gabes.helpdesk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.gabes.helpdesk.common.exception.ResourceNotFoundException;
import tn.iset.gabes.helpdesk.dto.request.EquipmentRequest;
import tn.iset.gabes.helpdesk.dto.response.EquipmentResponse;
import tn.iset.gabes.helpdesk.dto.response.PageResponse;
import tn.iset.gabes.helpdesk.entity.Equipment;
import tn.iset.gabes.helpdesk.entity.Establishment;
import tn.iset.gabes.helpdesk.entity.enums.EquipmentType;
import tn.iset.gabes.helpdesk.mapper.EquipmentMapper;
import tn.iset.gabes.helpdesk.repository.EquipmentRepository;
import tn.iset.gabes.helpdesk.repository.EstablishmentRepository;
import tn.iset.gabes.helpdesk.security.SecurityUtils;
import tn.iset.gabes.helpdesk.security.UserPrincipal;

import java.util.List;
import java.util.UUID;

/**
 * Gestion du parc de materiel et des etiquettes QR associees.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final EstablishmentRepository establishmentRepository;
    private final EquipmentMapper equipmentMapper;
    private final QrCodeService qrCodeService;

    @Transactional
    public EquipmentResponse create(EquipmentRequest request) {
        Establishment establishment = establishmentRepository.findById(request.establishmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Etablissement", request.establishmentId()));

        Equipment equipment = new Equipment();
        equipmentMapper.apply(request, equipment);
        equipment.setEstablishment(establishment);
        // Jeton public genere par le serveur : il sera encode dans l'etiquette QR
        equipment.setQrToken(UUID.randomUUID().toString());

        Equipment saved = equipmentRepository.save(equipment);
        log.info("Materiel enregistre : {} chez {}", saved.getName(), establishment.getName());
        return equipmentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<EquipmentResponse> search(Long establishmentId,
                                                  EquipmentType type,
                                                  String search,
                                                  Pageable pageable) {
        UserPrincipal principal = SecurityUtils.requireCurrentUser();

        // Un client ne voit que son propre parc, quel que soit le filtre demande
        Long effectiveEstablishmentId = principal.isClient()
                ? principal.getEstablishmentId()
                : establishmentId;

        Page<Equipment> page = equipmentRepository.search(
                effectiveEstablishmentId, type,
                (search == null || search.isBlank()) ? null : search.trim(),
                pageable);
        return PageResponse.from(page, equipmentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<EquipmentResponse> getByEstablishment(Long establishmentId) {
        UserPrincipal principal = SecurityUtils.requireCurrentUser();
        if (principal.isClient() && !establishmentId.equals(principal.getEstablishmentId())) {
            throw new AccessDeniedException(
                    "Vous ne pouvez consulter que le materiel de votre etablissement.");
        }
        return equipmentRepository.findByEstablishmentIdAndActiveTrue(establishmentId).stream()
                .map(equipmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EquipmentResponse getById(Long id) {
        Equipment equipment = loadEquipment(id);
        assertCanAccess(equipment);
        return equipmentMapper.toResponse(equipment);
    }

    /**
     * Resolution d'une etiquette QR scannee.
     * <p>
     * Le jeton ne revele rien d'exploitable par lui-meme : il faut en plus
     * etre authentifie et rattache a l'etablissement proprietaire pour
     * obtenir la fiche du materiel.
     */
    @Transactional(readOnly = true)
    public EquipmentResponse getByQrToken(String qrToken) {
        Equipment equipment = equipmentRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun materiel ne correspond a cette etiquette QR."));
        assertCanAccess(equipment);
        return equipmentMapper.toResponse(equipment);
    }

    /** Image PNG de l'etiquette QR, prete a imprimer et a coller sur la machine. */
    @Transactional(readOnly = true)
    public byte[] generateQrCodeImage(Long id, int size) {
        Equipment equipment = loadEquipment(id);
        assertCanAccess(equipment);
        return qrCodeService.generatePng(
                equipmentMapper.buildQrContent(equipment.getQrToken()), size);
    }

    @Transactional
    public EquipmentResponse update(Long id, EquipmentRequest request) {
        Equipment equipment = loadEquipment(id);
        equipmentMapper.apply(request, equipment);

        // Un deplacement de materiel vers un autre etablissement reste possible
        if (request.establishmentId() != null
                && !request.establishmentId().equals(equipment.getEstablishment().getId())) {
            Establishment target = establishmentRepository.findById(request.establishmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Etablissement", request.establishmentId()));
            equipment.setEstablishment(target);
        }

        return equipmentMapper.toResponse(equipmentRepository.save(equipment));
    }

    @Transactional
    public EquipmentResponse setActive(Long id, boolean active) {
        Equipment equipment = loadEquipment(id);
        equipment.setActive(active);
        return equipmentMapper.toResponse(equipmentRepository.save(equipment));
    }

    /**
     * Materiels les plus souvent en panne, utile pour conseiller un
     * remplacement plutot que des reparations repetees.
     *
     * @param establishmentId limite le classement a un client, null pour tous
     * @param limit           nombre de lignes retournees
     */
    @Transactional(readOnly = true)
    public List<MostProblematicEquipment> getMostProblematic(Long establishmentId, int limit) {
        UserPrincipal principal = SecurityUtils.requireCurrentUser();
        Long effectiveId = principal.isClient()
                ? principal.getEstablishmentId()
                : establishmentId;

        return equipmentRepository
                .findMostProblematic(effectiveId,
                        org.springframework.data.domain.PageRequest.of(0, Math.max(1, limit)))
                .stream()
                .map(row -> new MostProblematicEquipment(
                        equipmentMapper.toResponse((Equipment) row[0]),
                        ((Number) row[1]).longValue()))
                .toList();
    }

    /**
     * Materiel et nombre de pannes declarees.
     */
    public record MostProblematicEquipment(EquipmentResponse equipment, long ticketCount) {
    }

    private Equipment loadEquipment(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Materiel", id));
    }

    private void assertCanAccess(Equipment equipment) {
        UserPrincipal principal = SecurityUtils.requireCurrentUser();
        if (principal.isClient()
                && !equipment.getEstablishment().getId().equals(principal.getEstablishmentId())) {
            throw new ResourceNotFoundException("Materiel", equipment.getId());
        }
    }
}
