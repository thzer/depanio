package tn.iset.gabes.helpdesk.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tn.iset.gabes.helpdesk.entity.User;
import tn.iset.gabes.helpdesk.entity.enums.Role;
import tn.iset.gabes.helpdesk.repository.UserRepository;

/**
 * Creation du compte administrateur initial.
 * <p>
 * L'application demarre sur une base vide : aucun client, aucun materiel,
 * aucun ticket. Un seul compte est necessaire pour ouvrir la session et
 * commencer a saisir une activite reelle.
 * <p>
 * Ce compte n'est cree qu'a une condition : <b>la table des utilisateurs
 * est vide</b>. Passe le premier demarrage, ce composant ne fait plus
 * rien. Il ne peut donc ni ecraser un mot de passe existant, ni recreer un
 * compte supprime volontairement.
 * <p>
 * Le mot de passe est lu dans la configuration et n'apparait jamais dans
 * le code. En production, il provient d'une variable d'environnement ou de
 * Secret Manager, et peut etre change depuis l'application une fois la
 * premiere connexion effectuee.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminBootstrap {

    /** Longueur minimale exigee pour le mot de passe du compte initial. */
    private static final int MIN_PASSWORD_LENGTH = 10;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;
    private final HelpdeskProperties properties;

    @Bean
    public ApplicationRunner createInitialAdmin() {
        return args -> {
            if (userRepository.count() > 0) {
                log.debug("Des comptes existent deja : aucun compte initial n'est cree.");
                return;
            }

            HelpdeskProperties.Admin admin = properties.getAdmin();
            String email = admin.getEmail();
            String password = admin.getPassword();

            if (isBlank(email) || isBlank(password)) {
                log.warn("""

                        ============================================================
                         La base est vide et aucun compte administrateur n'est
                         configure. Renseignez ces deux variables, puis redemarrez :

                           HELPDESK_ADMIN_EMAIL
                           HELPDESK_ADMIN_PASSWORD

                         Aucun compte par defaut n'est cree : un mot de passe connu
                         d'avance ouvrirait l'acces a toutes les donnees.
                        ============================================================
                        """);
                return;
            }

            if (password.length() < MIN_PASSWORD_LENGTH) {
                log.error("Le mot de passe administrateur doit contenir au moins {} "
                        + "caracteres. Aucun compte n'a ete cree.", MIN_PASSWORD_LENGTH);
                return;
            }

            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                User created = userRepository.save(User.builder()
                        .fullName(orDefault(admin.getFullName(), "Administrateur"))
                        .email(email.trim().toLowerCase())
                        .password(passwordEncoder.encode(password))
                        .phone(admin.getPhone())
                        .role(Role.ADMIN)
                        .active(true)
                        .build());

                log.info("""

                        ============================================================
                         Compte administrateur cree : {}

                         La base ne contient rien d'autre. Connectez-vous pour
                         enregistrer vos premiers etablissements clients.
                        ============================================================
                        """, created.getEmail());
            });
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String orDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }
}
