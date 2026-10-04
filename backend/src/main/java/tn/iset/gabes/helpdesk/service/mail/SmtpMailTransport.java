package tn.iset.gabes.helpdesk.service.mail;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import tn.iset.gabes.helpdesk.config.HelpdeskProperties;

import java.nio.charset.StandardCharsets;

/**
 * Acheminement par le protocole SMTP.
 * <p>
 * C'est le moyen a retenir pour une installation chez le client ou sur un
 * serveur loue : il n'enferme dans aucun fournisseur et accepte aussi bien
 * Gmail que le serveur interne d'une societe. Il suppose en revanche que le
 * port sortant soit ouvert, ce que les hebergements gratuits refusent
 * presque toujours : c'est pourquoi il passe apres {@link HttpMailTransport}.
 */
@Order(2)
@Component
public class SmtpMailTransport implements MailTransport {

    private final ObjectProvider<JavaMailSender> sender;
    private final HelpdeskProperties properties;
    private final String host;

    public SmtpMailTransport(ObjectProvider<JavaMailSender> sender,
                             HelpdeskProperties properties,
                             @Value("${spring.mail.host:}") String host) {
        // ObjectProvider plutot qu'injection directe : le bean d'envoi
        // n'existe que si un serveur est configure, et son absence ne doit
        // pas empecher l'application de demarrer.
        this.sender = sender;
        this.properties = properties;
        this.host = host;
    }

    /**
     * Le nom du serveur est verifie en plus de la presence du bean
     * d'envoi.
     * <p>
     * La nuance a son importance : lorsque la variable {@code MAIL_HOST}
     * n'est pas renseignee, elle vaut la chaine vide et non rien du tout.
     * Spring considere alors la propriete comme presente et construit tout
     * de meme le bean d'envoi. Sans ce controle, ce transport se declarait
     * disponible sans serveur a joindre : il etait retenu, et chaque
     * courriel echouait sur une adresse vide.
     */
    @Override
    public boolean isAvailable() {
        return host != null && !host.isBlank() && sender.getIfAvailable() != null;
    }

    @Override
    public void send(String to, String subject, String htmlBody) throws Exception {
        JavaMailSender courrielier = sender.getIfAvailable();
        if (courrielier == null) {
            throw new IllegalStateException("aucun serveur SMTP configure");
        }

        MimeMessage message = courrielier.createMimeMessage();
        MimeMessageHelper aide =
                new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

        aide.setFrom(properties.getMail().getFrom(), properties.getMail().getFromName());
        aide.setTo(to);
        aide.setSubject(subject);
        aide.setText(htmlBody, true);

        courrielier.send(message);
    }

    @Override
    public String name() {
        return "SMTP";
    }
}
