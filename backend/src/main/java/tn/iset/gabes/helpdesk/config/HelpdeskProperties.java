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

    /** Adresse publique de l'application, encodee dans les QR codes. */
    private String frontendUrl = "http://localhost:4200";

    /**
     * Adresse publique normalisee.
     * <p>
     * Certains hebergeurs fournissent seulement le nom d'hote, sans
     * protocole. Un QR code contenant {@code exemple.com/scan/...} ne
     * s'ouvrirait pas au scan : le protocole est donc ajoute si besoin,
     * et la barre oblique finale retiree pour eviter les doublons.
     */
    public String getFrontendUrl() {
        if (frontendUrl == null || frontendUrl.isBlank()) {
            return "";
        }
        String value = frontendUrl.trim();
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = (value.startsWith("localhost") ? "http://" : "https://") + value;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private Security security = new Security();
    private Sla sla = new Sla();
    private Cors cors = new Cors();
    private Admin admin = new Admin();
    private Mail mail = new Mail();
    private KeepAlive keepAlive = new KeepAlive();

    /**
     * Envoi de courriels.
     * <p>
     * Les notifications dans l'application ne suffisent pas : un technicien
     * en deplacement n'a pas le navigateur ouvert. Le courriel est le seul
     * canal qui le joigne sans rien installer ni rien payer.
     * <p>
     * Desactive tant qu'aucun serveur n'est configure : l'application
     * fonctionne alors normalement, sans courriel.
     */
    @Getter
    @Setter
    public static class Mail {
        private boolean enabled = false;

        /** Adresse affichee comme expediteur. */
        private String from = "";

        /** Nom affiche a cote de l'adresse. */
        private String fromName = "Depanio";
    }

    /**
     * Maintien en eveil du service.
     * <p>
     * Sur une offre d'hebergement gratuite, le service est mis en veille
     * apres quelques minutes sans visite. Or la surveillance des delais
     * contractuels est une tache de fond : endormie, elle ne constate plus
     * aucun depassement a l'heure. Une requete periodique sur sa propre
     * adresse publique maintient le service actif.
     */
    @Getter
    @Setter
    public static class KeepAlive {
        private boolean enabled = false;

        /** Frequence de la requete d'eveil, au format cron. */
        private String cron = "0 */10 * * * *";
    }

    /**
     * Compte administrateur cree au tout premier demarrage, lorsque la
     * base est encore vide.
     * <p>
     * Aucune valeur par defaut : un compte administrateur dont le mot de
     * passe serait connu d'avance ouvrirait l'acces a toutes les donnees.
     */
    @Getter
    @Setter
    public static class Admin {
        private String email;
        private String password;
        private String fullName = "Administrateur";
        private String phone;
    }

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
