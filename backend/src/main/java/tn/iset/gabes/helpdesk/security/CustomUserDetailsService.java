package tn.iset.gabes.helpdesk.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.gabes.helpdesk.repository.UserRepository;

/**
 * Chargement de l'utilisateur a partir de son adresse electronique.
 * <p>
 * Spring Security appelle ce service au moment de l'authentification par
 * mot de passe, puis le filtre JWT l'utilise a chaque requete pour
 * reconstituer le contexte de securite.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCase(email)
                .map(UserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Aucun compte ne correspond a l'adresse " + email));
    }
}
