package tn.iset.gabes.helpdesk.service.mail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import tn.iset.gabes.helpdesk.config.HelpdeskProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Disponibilite du transport SMTP.
 * <p>
 * Ces trois cas couvrent un piege rencontre en production. Lorsque la
 * variable d'environnement {@code MAIL_HOST} n'est pas renseignee, elle
 * vaut la chaine vide et non rien du tout : Spring considere alors la
 * propriete comme presente et construit tout de meme le bean d'envoi. Le
 * transport se declarait disponible sans serveur a joindre, etait retenu,
 * et chaque courriel echouait silencieusement.
 */
class SmtpMailTransportTest {

    @Test
    @DisplayName("Un serveur configure rend le transport disponible")
    void serveurConfigure() {
        assertThat(transport("smtp.exemple.tn", true).isAvailable()).isTrue();
    }

    @Test
    @DisplayName("Un nom de serveur vide ne suffit pas, meme avec le bean d envoi")
    void nomDeServeurVide() {
        assertThat(transport("", true).isAvailable()).isFalse();
        assertThat(transport("   ", true).isAvailable()).isFalse();
    }

    @Test
    @DisplayName("Sans bean d envoi, le transport reste indisponible")
    void sansBeanDEnvoi() {
        assertThat(transport("smtp.exemple.tn", false).isAvailable()).isFalse();
    }

    private SmtpMailTransport transport(String host, boolean avecBean) {
        return new SmtpMailTransport(
                fournisseur(avecBean ? new JavaMailSenderImpl() : null),
                new HelpdeskProperties(),
                host);
    }

    /** Fournisseur minimal : seul getIfAvailable() est sollicite ici. */
    private ObjectProvider<JavaMailSender> fournisseur(JavaMailSender valeur) {
        return new ObjectProvider<>() {
            @Override public JavaMailSender getObject() {
                return valeur;
            }
            @Override public JavaMailSender getObject(Object... args) {
                return valeur;
            }
            @Override public JavaMailSender getIfAvailable() {
                return valeur;
            }
            @Override public JavaMailSender getIfUnique() {
                return valeur;
            }
        };
    }
}
