package tn.iset.gabes.helpdesk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.gabes.helpdesk.common.exception.BusinessException;
import tn.iset.gabes.helpdesk.common.exception.ResourceNotFoundException;
import tn.iset.gabes.helpdesk.dto.request.CreateUserRequest;
import tn.iset.gabes.helpdesk.dto.request.UpdateUserRequest;
import tn.iset.gabes.helpdesk.dto.response.PageResponse;
import tn.iset.gabes.helpdesk.dto.response.TechnicianWorkloadResponse;
import tn.iset.gabes.helpdesk.dto.response.UserResponse;
import tn.iset.gabes.helpdesk.entity.Establishment;
import tn.iset.gabes.helpdesk.entity.User;
import tn.iset.gabes.helpdesk.entity.enums.Role;
import tn.iset.gabes.helpdesk.entity.enums.TicketStatus;
import tn.iset.gabes.helpdesk.mapper.UserMapper;
import tn.iset.gabes.helpdesk.repository.EstablishmentRepository;
import tn.iset.gabes.helpdesk.repository.TicketRepository;
import tn.iset.gabes.helpdesk.repository.UserRepository;
import tn.iset.gabes.helpdesk.security.SecurityUtils;

import java.util.List;

/**
 * Gestion des comptes utilisateurs.
 * <p>
 * Les comptes ne sont jamais supprimes mais desactives : leurs tickets et
 * leurs commentaires doivent rester rattaches a leur auteur, ce qui est
 * indispensable a la valeur probante de l'historique.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final List<TicketStatus> CLOSED_STATUSES =
            List.of(TicketStatus.CLOSED, TicketStatus.CANCELLED);

    private final UserRepository userRepository;
    private final EstablishmentRepository establishmentRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(
                    "L'adresse " + email + " est deja utilisee par un autre compte.");
        }

        Establishment establishment = null;
        if (request.role() == Role.CLIENT) {
            if (request.establishmentId() == null) {
                throw new BusinessException(
                        "Un compte client doit etre rattache a un etablissement.");
            }
            establishment = establishmentRepository.findById(request.establishmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Etablissement", request.establishmentId()));
        }

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .role(request.role())
                .establishment(establishment)
                .speciality(request.role() == Role.TECHNICIAN ? request.speciality() : null)
                .active(true)
                .build();

        User saved = userRepository.save(user);
        log.info("Compte cree : {} ({})", saved.getEmail(), saved.getRole());
        return userMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String search, Role role, Pageable pageable) {
        Page<User> page = userRepository.search(
                (search == null || search.isBlank()) ? null : search.trim(), role, pageable);
        return PageResponse.from(page, userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getActiveTechnicians() {
        return userRepository.findByRoleAndActiveTrue(Role.TECHNICIAN).stream()
                .map(userMapper::toResponse)
                .toList();
    }

    /**
     * Techniciens classes du moins charge au plus charge, avec le nombre de
     * tickets ouverts et de depassements. Aide l'administrateur a repartir
     * la charge au moment de l'affectation.
     */
    @Transactional(readOnly = true)
    public List<TechnicianWorkloadResponse> getTechnicianWorkload() {
        return userRepository.findTechniciansByWorkload().stream()
                .map(technician -> new TechnicianWorkloadResponse(
                        technician.getId(),
                        technician.getFullName(),
                        technician.getSpeciality(),
                        ticketRepository.countByAssignedToIdAndStatusNotIn(
                                technician.getId(), CLOSED_STATUSES),
                        0L))
                .toList();
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));

        if (request.email() != null && !request.email().isBlank()) {
            String email = request.email().trim().toLowerCase();
            boolean takenByAnother = userRepository.findByEmailIgnoreCase(email)
                    .filter(existing -> !existing.getId().equals(id))
                    .isPresent();
            if (takenByAnother) {
                throw new BusinessException(
                        "L'adresse " + email + " est deja utilisee par un autre compte.");
            }
            user.setEmail(email);
        }
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.speciality() != null && user.getRole() == Role.TECHNICIAN) {
            user.setSpeciality(request.speciality());
        }
        if (request.active() != null) {
            applyActiveFlag(user, request.active());
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    /**
     * Active ou desactive un compte.
     * <p>
     * Un administrateur ne peut pas desactiver son propre compte : il
     * perdrait immediatement l'acces et plus personne ne pourrait le
     * reactiver si c'est le seul administrateur.
     */
    @Transactional
    public UserResponse setActive(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
        applyActiveFlag(user, active);
        return userMapper.toResponse(userRepository.save(user));
    }

    private void applyActiveFlag(User user, boolean active) {
        if (!active && user.getId().equals(SecurityUtils.getCurrentUserId())) {
            throw new BusinessException("Vous ne pouvez pas desactiver votre propre compte.");
        }
        if (!active && user.getRole() == Role.ADMIN
                && userRepository.findByRoleAndActiveTrue(Role.ADMIN).size() <= 1) {
            throw new BusinessException(
                    "Le dernier administrateur actif ne peut pas etre desactive.");
        }
        if (!active && user.getRole() == Role.TECHNICIAN) {
            long open = ticketRepository.countByAssignedToIdAndStatusNotIn(
                    user.getId(), CLOSED_STATUSES);
            if (open > 0) {
                throw new BusinessException(
                        "Ce technicien a encore " + open + " ticket(s) en cours. "
                                + "Reaffectez-les avant de desactiver le compte.");
            }
        }
        user.setActive(active);
    }

    /** Reinitialisation du mot de passe par un administrateur. */
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        if (newPassword == null || newPassword.length() < 10) {
            throw new BusinessException(
                    "Le mot de passe doit contenir au moins 10 caracteres.");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Mot de passe reinitialise pour {}", user.getEmail());
    }
}
