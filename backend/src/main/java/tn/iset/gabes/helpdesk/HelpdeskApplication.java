package tn.iset.gabes.helpdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entree de l'application.
 *
 * <ul>
 *   <li>{@code @EnableJpaAuditing} renseigne automatiquement les dates de
 *       creation et de modification des entites.</li>
 *   <li>{@code @EnableScheduling} active la surveillance periodique des
 *       delais contractuels (SLA).</li>
 *   <li>{@code @EnableAsync} permet d'envoyer les notifications sans
 *       bloquer la reponse HTTP de l'utilisateur.</li>
 *   <li>{@code @ConfigurationPropertiesScan} charge les parametres metier
 *       declares dans {@code HelpdeskProperties}.</li>
 * </ul>
 *
 * Projet de Fin d'Etudes - ISET Gabes
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
@ConfigurationPropertiesScan
public class HelpdeskApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelpdeskApplication.class, args);
    }
}
