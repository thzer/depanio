package tn.iset.gabes.helpdesk.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tn.iset.gabes.helpdesk.dto.request.EstablishmentRequest;
import tn.iset.gabes.helpdesk.dto.response.EstablishmentResponse;
import tn.iset.gabes.helpdesk.entity.Establishment;
import tn.iset.gabes.helpdesk.repository.TicketRepository;

/**
 * Conversion des etablissements.
 * <p>
 * Le nombre de tickets ouverts est lu au moment de la conversion : cette
 * valeur change constamment et n'a donc pas de sens stockee dans l'entite.
 */
@Component
@RequiredArgsConstructor
public class EstablishmentMapper {

    private final TicketRepository ticketRepository;

    public EstablishmentResponse toResponse(Establishment establishment) {
        if (establishment == null) {
            return null;
        }
        return new EstablishmentResponse(
                establishment.getId(),
                establishment.getName(),
                establishment.getType(),
                establishment.getType().getLabel(),
                establishment.getAddress(),
                establishment.getCity(),
                establishment.getPhone(),
                establishment.getContactName(),
                establishment.getContactEmail(),
                establishment.getContractNumber(),
                establishment.isActive(),
                establishment.getEquipments() != null ? establishment.getEquipments().size() : 0,
                ticketRepository.countOpen(establishment.getId()),
                establishment.getCreatedAt()
        );
    }

    /** Applique les champs d'une requete sur une entite, nouvelle ou existante. */
    public void apply(EstablishmentRequest request, Establishment target) {
        target.setName(request.name());
        target.setType(request.type());
        target.setAddress(request.address());
        target.setCity(request.city());
        target.setPhone(request.phone());
        target.setContactName(request.contactName());
        target.setContactEmail(request.contactEmail());
        target.setContractNumber(request.contractNumber());
        if (request.active() != null) {
            target.setActive(request.active());
        }
    }
}
