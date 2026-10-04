package tn.iset.gabes.helpdesk.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tn.iset.gabes.helpdesk.config.HelpdeskProperties;
import tn.iset.gabes.helpdesk.entity.User;
import tn.iset.gabes.helpdesk.entity.enums.NotificationType;
import tn.iset.gabes.helpdesk.service.mail.MailTransport;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Choix du moyen d'acheminement des courriels.
 * <p>
 * Ce choix a une consequence concrete : sur un hebergement gratuit, le
 * protocole SMTP est bloque et le courriel se perd apres dix secondes
 * d'attente. Une erreur de priorite ne produirait donc aucune exception,
 * seulement des notifications qui n'arrivent jamais. D'ou ces tests.
 */
class EmailTransportSelectionTest {

    @Test
    @DisplayName("La requete HTTPS passe avant le SMTP quand les deux sont disponibles")
    void httpsPrioritaire() {
        TransportTemoin https = new TransportTemoin("HTTPS", true);
        TransportTemoin smtp = new TransportTemoin("SMTP", true);

        envoyer(new EmailService(List.of(https, smtp), configure()));

        assertThat(https.appele).isTrue();
        assertThat(smtp.appele).isFalse();
    }

    @Test
    @DisplayName("Le SMTP sert de repli lorsque aucune cle d interface n est fournie")
    void repliSurSmtp() {
        TransportTemoin https = new TransportTemoin("HTTPS", false);
        TransportTemoin smtp = new TransportTemoin("SMTP", true);

        envoyer(new EmailService(List.of(https, smtp), configure()));

        assertThat(smtp.appele).isTrue();
    }

    @Test
    @DisplayName("Sans moyen d envoi, rien ne part et rien n echoue")
    void aucunMoyen() {
        TransportTemoin https = new TransportTemoin("HTTPS", false);
        TransportTemoin smtp = new TransportTemoin("SMTP", false);

        EmailService service = new EmailService(List.of(https, smtp), configure());

        assertThat(service.concerne(NotificationType.TICKET_ASSIGNED)).isFalse();
        envoyer(service);
        assertThat(https.appele).isFalse();
        assertThat(smtp.appele).isFalse();
    }

    @Test
    @DisplayName("Courriels desactives : aucun envoi meme avec un moyen disponible")
    void desactive() {
        TransportTemoin https = new TransportTemoin("HTTPS", true);

        HelpdeskProperties proprietes = configure();
        proprietes.getMail().setEnabled(false);

        envoyer(new EmailService(List.of(https), proprietes));

        assertThat(https.appele).isFalse();
    }

    @Test
    @DisplayName("Un echec d acheminement ne remonte pas a l appelant")
    void echecSilencieux() {
        MailTransport cassse = new MailTransport() {
            @Override public boolean isAvailable() {
                return true;
            }
            @Override public void send(String to, String subject, String html) {
                throw new IllegalStateException("serveur injoignable");
            }
            @Override public String name() {
                return "HTTPS";
            }
        };

        // Aucune exception attendue : une panne de messagerie ne doit
        // jamais empecher d'enregistrer une intervention.
        envoyer(new EmailService(List.of(cassse), configure()));
    }

    @Test
    @DisplayName("Seuls les motifs importants donnent lieu a un courriel")
    void motifsRestreints() {
        EmailService service =
                new EmailService(List.of(new TransportTemoin("HTTPS", true)), configure());

        assertThat(service.concerne(NotificationType.TICKET_ASSIGNED)).isTrue();
        assertThat(service.concerne(NotificationType.SLA_BREACHED)).isTrue();
        // Un courriel par commentaire finirait en regle de filtrage, et
        // les alertes urgentes seraient perdues avec le reste.
        assertThat(service.concerne(NotificationType.NEW_COMMENT)).isFalse();
    }

    // ------------------------------------------------------------- Outils

    private void envoyer(EmailService service) {
        User destinataire = new User();
        destinataire.setEmail("technicien@exemple.tn");
        destinataire.setFullName("Technicien");

        service.envoyer(destinataire, NotificationType.TICKET_ASSIGNED, "Message", null);
    }

    private HelpdeskProperties configure() {
        HelpdeskProperties proprietes = new HelpdeskProperties();
        proprietes.getMail().setEnabled(true);
        proprietes.getMail().setFrom("depanio@exemple.tn");
        return proprietes;
    }

    /** Transport qui note seulement qu'il a ete sollicite. */
    private static final class TransportTemoin implements MailTransport {
        private final String nom;
        private final boolean disponible;
        private boolean appele;

        private TransportTemoin(String nom, boolean disponible) {
            this.nom = nom;
            this.disponible = disponible;
        }

        @Override public boolean isAvailable() {
            return disponible;
        }

        @Override public void send(String to, String subject, String html) {
            appele = true;
        }

        @Override public String name() {
            return nom;
        }
    }
}
