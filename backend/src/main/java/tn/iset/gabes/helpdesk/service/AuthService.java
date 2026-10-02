package tn.iset.gabes.helpdesk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.gabes.helpdesk.common.exception.BusinessException;
import tn.iset.gabes.helpdesk.common.exception.ResourceNotFoundException;
import tn.iset.gabes.helpdesk.dto.request.ChangePasswordRequest;
import tn.iset.gabes.helpdesk.dto.request.LoginRequest;
import tn.iset.gabes.helpdesk.dto.request.RefreshTokenRequest;
import tn.iset.gabes.helpdesk.dto.response.AuthResponse;
import tn.iset.gabes.helpdesk.dto.response.UserResponse;
import tn.iset.gabes.helpdesk.entity.User;
import tn.iset.gabes.helpdesk.mapper.UserMapper;
import tn.iset.gabes.helpdesk.repository.UserRepository;
import tn.iset.gabes.helpdesk.security.JwtService;
import tn.iset.gabes.helpdesk.security.LoginAttemptService;
import tn.iset.gabes.helpdesk.security.SecurityUtils;
import tn.iset.gabes.helpdesk.security.UserPrincipal;

/**
 * Authentification et gestion du mot de passe.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final LoginAttemptService loginAttemptService;

    /**
     * Verifie les identifiants et delivre les jetons.
     * <p>
     * Le message d'erreur reste identique que l'adresse soit inconnue ou
     * le mot de passe faux : distinguer les deux cas permettrait de
     * determiner quelles adresses existent dans le systeme.
     * <p>
     * Les echecs repetes sont comptes, par adresse visee et par origine.
     * Sans cela, rien n'empecherait d'essayer des milliers de mots de
     * passe : aucun mot de passe choisi par un humain n'y resisterait.
     *
     * @param origine adresse reseau de l'appelant, pour le comptage
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request, String origine) {
        String email = request.email();

        refuserSiBloque(email);
        refuserSiBloque(origine);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            loginAttemptService.reussite(email);
            loginAttemptService.reussite(origine);

            log.info("Connexion reussie : {} ({})", principal.getEmail(), principal.getRole());
            return buildAuthResponse(principal);

        } catch (DisabledException ex) {
            // Un compte desactive n'est pas une erreur d'identifiants :
            // le compter comme telle bloquerait un utilisateur legitime
            // qui ignore simplement que son acces a ete suspendu.
            throw new BusinessException(
                    "Ce compte est desactive. Contactez votre administrateur.");

        } catch (BadCredentialsException ex) {
            loginAttemptService.echec(email);
            loginAttemptService.echec(origine);
            log.warn("Echec de connexion pour une adresse");
            throw new BusinessException("Adresse electronique ou mot de passe incorrect.");
        }
    }

    /**
     * Interrompt la tentative si la cle est sous blocage, en indiquant le
     * delai restant : un utilisateur legitime qui s'est trompe doit savoir
     * quand reessayer, plutot que de buter sur un refus inexplique.
     */
    private void refuserSiBloque(String cle) {
        if (cle != null && loginAttemptService.estBloque(cle)) {
            throw new BusinessException(
                    "Trop de tentatives. Reessayez dans "
                            + loginAttemptService.minutesRestantes(cle) + " minutes.");
        }
    }

    /**
     * Delivre un nouveau jeton d'acces a partir d'un jeton de
     * renouvellement encore valide.
     */
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new BusinessException(
                    "Le jeton de renouvellement est invalide ou expire. Reconnectez-vous.");
        }
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BusinessException("Le jeton fourni n'est pas un jeton de renouvellement.");
        }

        String email = jwtService.extractEmail(refreshToken);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun compte ne correspond a ce jeton."));

        if (!user.isActive()) {
            throw new BusinessException("Ce compte est desactive.");
        }

        return buildAuthResponse(new UserPrincipal(user));
    }

    /** Profil de l'utilisateur connecte. */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        return userRepository.findById(userId)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));
    }

    /**
     * Changement de mot de passe par l'utilisateur lui-meme.
     * L'ancien mot de passe est verifie avant toute modification.
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException("Le mot de passe actuel est incorrect.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException(
                    "Le nouveau mot de passe doit etre different de l'actuel.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.info("Mot de passe modifie pour l'utilisateur {}", user.getEmail());
    }

    private AuthResponse buildAuthResponse(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", principal.getId()));

        return AuthResponse.of(
                jwtService.generateAccessToken(principal),
                jwtService.generateRefreshToken(principal),
                jwtService.getAccessTokenValiditySeconds(),
                userMapper.toResponse(user)
        );
    }
}
