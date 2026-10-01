package tn.iset.gabes.helpdesk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.gabes.helpdesk.common.exception.BusinessException;
import tn.iset.gabes.helpdesk.common.exception.ResourceNotFoundException;
import tn.iset.gabes.helpdesk.dto.request.EstablishmentRequest;
import tn.iset.gabes.helpdesk.dto.response.EstablishmentResponse;
import tn.iset.gabes.helpdesk.dto.response.PageResponse;
import tn.iset.gabes.helpdesk.entity.Establishment;
import tn.iset.gabes.helpdesk.entity.enums.EstablishmentType;
import tn.iset.gabes.helpdesk.mapper.EstablishmentMapper;
import tn.iset.gabes.helpdesk.repository.EstablishmentRepository;
import tn.iset.gabes.helpdesk.repository.TicketRepository;
import tn.iset.gabes.helpdesk.security.UserPrincipal;
import tn.iset.gabes.helpdesk.security.SecurityUtils;

import java.util.List;

/**
 * Gestion du portefeuille de clients.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EstablishmentService {

    private final EstablishmentRepository establishmentRepository;
    private final TicketRepository ticketRepository;
    private final EstablishmentMapper establishmentMapper;

    @Transactional
    public EstablishmentResponse create(EstablishmentRequest request) {
        if (request.contractNumber() != null && !request.contractNumber().isBlank()
                && establishmentRepository.existsByContractNumber(request.contractNumber())) {
            throw new BusinessException(
                    "Le numero de contrat " + request.contractNumber() + " est deja enregistre.");
        }

        Establishment establishment = new Establishment();
        establishmentMapper.apply(request, establishment);
        Establishment saved = establishmentRepository.save(establishment);

        log.info("Etablissement cree : {} ({})", saved.getName(), saved.getCity());
        return establishmentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<EstablishmentResponse> search(String search,
                                                      EstablishmentType type,
                                                      Boolean active,
                                                      Pageable pageable) {
        Page<Establishment> page = establishmentRepository.search(
                (search == null || search.isBlank()) ? null : search.trim(),
                type, active, pageable);
        return PageResponse.from(page, establishmentMapper::toResponse);
    }

    /** Liste simplifiee des etablissements actifs, pour les listes deroulantes. */
    @Transactional(readOnly = true)
    public List<EstablishmentResponse> getActive() {
        return establishmentRepository.findByActiveTrue().stream()
                .map(establishmentMapper::toResponse)
                .toList();
    }

    /**
     * Fiche d'un etablissement.
     * Un CLIENT ne peut consulter que celle de son propre etablissement.
     */
    @Transactional(readOnly = true)
    public EstablishmentResponse getById(Long id) {
        UserPrincipal principal = SecurityUtils.requireCurrentUser();
        if (principal.isClient() && !id.equals(principal.getEstablishmentId())) {
            throw new AccessDeniedException(
                    "Vous ne pouvez consulter que votre propre etablissement.");
        }
        return establishmentRepository.findById(id)
                .map(establishmentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Etablissement", id));
    }

    @Transactional
    public EstablishmentResponse update(Long id, EstablishmentRequest request) {
        Establishment establishment = establishmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Etablissement", id));
        establishmentMapper.apply(request, establishment);
        return establishmentMapper.toResponse(establishmentRepository.save(establishment));
    }

    /**
     * Desactive un etablissement.
     * <p>
     * La desactivation est refusee s'il reste des tickets ouverts : ces
     * demandes doivent etre traitees ou annulees avant la fin du contrat.
     */
    @Transactional
    public EstablishmentResponse setActive(Long id, boolean active) {
        Establishment establishment = establishmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Etablissement", id));

        if (!active) {
            long openTickets = ticketRepository.countOpen(id);
            if (openTickets > 0) {
                throw new BusinessException(
                        "Cet etablissement a encore " + openTickets + " ticket(s) ouvert(s). "
                                + "Cloturez-les avant de le desactiver.");
            }
        }

        establishment.setActive(active);
        return establishmentMapper.toResponse(establishmentRepository.save(establishment));
    }
}
