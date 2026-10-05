package tn.iset.gabes.helpdesk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.gabes.helpdesk.common.exception.BusinessException;
import tn.iset.gabes.helpdesk.common.exception.ResourceNotFoundException;
import tn.iset.gabes.helpdesk.dto.request.DemoRequestForm;
import tn.iset.gabes.helpdesk.dto.response.DemoRequestResponse;
import tn.iset.gabes.helpdesk.dto.response.PageResponse;
import tn.iset.gabes.helpdesk.entity.DemoRequest;
import tn.iset.gabes.helpdesk.entity.enums.NotificationType;
import tn.iset.gabes.helpdesk.entity.enums.Role;
import tn.iset.gabes.helpdesk.repository.DemoRequestRepository;
import tn.iset.gabes.helpdesk.repository.UserRepository;

import java.time.LocalDateTime;

/**
 * Demandes de demonstration laissees depuis la page commerciale.
 * <p>
 * Le point d'entree de depot est public : il faut donc le proteger des
 * envois automatises, sans pour autant dresser un obstacle devant un
 * prospect. Trois garde-fous discrets sont combines :
 * <ol>
 *   <li>un champ piege, invisible et laisse vide par un humain ;</li>
 *   <li>une limite par adresse, qui ecarte les envois repetes ;</li>
 *   <li>une limite globale par heure, qui borne les degats d'un robot
 *       changeant d'adresse a chaque envoi.</li>
 * </ol>
 * Aucun de ces controles n'est visible pour un visiteur legitime.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemoRequestService {

    /** Demandes acceptees pour une meme adresse sur la periode. */
    private static final int MAX_PAR_ADRESSE = 3;

    /** Demandes acceptees au total sur la periode, toutes adresses confondues. */
    private static final int MAX_GLOBAL = 40;

    private static final java.time.Duration PERIODE = java.time.Duration.ofHours(1);

    private final DemoRequestRepository demoRequestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Enregistre une demande et previent les administrateurs.
     *
     * @throws BusinessException si la demande parait automatisee
     */
    @Transactional
    public void submit(DemoRequestForm form) {
        // Un humain ne voit pas ce champ et ne peut donc pas le remplir.
        // On repond comme si tout s etait bien passe : signaler le piege
        // apprendrait au robot a le contourner.
        if (form.website() != null && !form.website().isBlank()) {
            log.debug("Demande de demonstration ignoree : champ piege rempli.");
            return;
        }

        LocalDateTime depuis = LocalDateTime.now().minus(PERIODE);
        String email = form.email().trim().toLowerCase();

        if (demoRequestRepository.countByEmailIgnoreCaseAndCreatedAtAfter(email, depuis)
                >= MAX_PAR_ADRESSE) {
            throw new BusinessException(
                    "Nous avons deja recu votre demande. Nous vous repondons tres vite.");
        }

        if (demoRequestRepository.countByCreatedAtAfter(depuis) >= MAX_GLOBAL) {
            log.warn("Limite horaire des demandes de demonstration atteinte.");
            throw new BusinessException(
                    "Le formulaire est momentanément indisponible. Réessayez dans un moment.");
        }

        DemoRequest demande = demoRequestRepository.save(DemoRequest.builder()
                .contactName(form.contactName().trim())
                .email(email)
                .phone(nullIfBlank(form.phone()))
                .companyName(nullIfBlank(form.companyName()))
                .establishmentCount(nullIfBlank(form.establishmentCount()))
                .message(nullIfBlank(form.message()))
                .handled(false)
                .build());

        log.info("Demande de demonstration recue de {} ({})",
                demande.getContactName(), demande.getEmail());

        notifierAdministrateurs(demande);
    }

    /**
     * Previent chaque administrateur, dans l'application et par courriel
     * si la messagerie est configuree.
     */
    private void notifierAdministrateurs(DemoRequest demande) {
        String societe = demande.getCompanyName() != null
                ? demande.getCompanyName()
                : demande.getEmail();

        String message = "Demande de demonstration : " + demande.getContactName()
                + " — " + societe;

        userRepository.findByRoleAndActiveTrue(Role.ADMIN).forEach(admin ->
                notificationService.notifyUser(
                        admin, NotificationType.TICKET_CREATED, message, null));
    }

    @Transactional(readOnly = true)
    public PageResponse<DemoRequestResponse> list(boolean onlyPending, Pageable pageable) {
        Page<DemoRequest> page = onlyPending
                ? demoRequestRepository.findByHandledFalseOrderByCreatedAtDesc(pageable)
                : demoRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PageResponse.from(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return demoRequestRepository.countByHandledFalse();
    }

    @Transactional
    public DemoRequestResponse markHandled(Long id) {
        DemoRequest demande = demoRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande", id));
        demande.markHandled();
        return toResponse(demoRequestRepository.save(demande));
    }

    private DemoRequestResponse toResponse(DemoRequest d) {
        return new DemoRequestResponse(
                d.getId(), d.getContactName(), d.getEmail(), d.getPhone(),
                d.getCompanyName(), d.getEstablishmentCount(), d.getMessage(),
                d.isHandled(), d.getHandledAt(), d.getCreatedAt());
    }

    private String nullIfBlank(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
