package tn.iset.gabes.helpdesk.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Parametres metier de l'application, lus depuis {@code application.yml}
 * sous la cle {@code helpdesk}.
 * <p>
 * Regrouper ces valeurs dans une classe typee evite de disperser des
 * annotations {@code @Value} dans tout le code et permet de modifier les
 * delais contractuels sans recompiler l'application.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "helpdesk")
public class HelpdeskProperties {

    /** URL publique du frontend, utilisee pour construire le contenu des QR codes. */
    private String frontendUrl = "http://localhost:4200";

    private Security security = new Security();
    private Sla sla = new Sla();
    private Cors cors = new Cors();

    @Getter
    @Setter
    public static class Security {
        private Jwt jwt = new Jwt();
    }

    @Getter
    @Setter
    public static class Jwt {
        /** Cle de signature encodee en Base64. */
        private String secret;
        /** Duree de vie du token d'acces, en millisecondes. */
        private long expiration = 86_400_000L;
        /** Duree de vie du token de renouvellement, en millisecondes. */
        private long refreshExpiration = 604_800_000L;
    }

    /**
     * Delais de resolution contractuels, exprimes en heures et indexes par
     * niveau de priorite.
     */
    @Getter
    @Setter
    public static class Sla {
        private int critical = 2;
        private int high = 8;
        private int medium = 24;
        private int low = 72;
        /** Frequence de verification des echeances, au format cron. */
        private String checkCron = "0 */5 * * * *";
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:4200");
    }
}
