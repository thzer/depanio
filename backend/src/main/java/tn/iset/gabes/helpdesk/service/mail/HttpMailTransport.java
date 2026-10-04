package tn.iset.gabes.helpdesk.service.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tn.iset.gabes.helpdesk.config.HelpdeskProperties;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Acheminement par requete HTTPS.
 * <p>
 * Raison d'etre : les hebergeurs mutualises ferment les ports sortants du
 * protocole SMTP (25, 465, 587) pour ne pas devenir des relais de courriels
 * non sollicites. Sur une telle plateforme, un envoi SMTP n'echoue pas avec
 * un refus clair : la connexion expire au bout de dix secondes, et la panne
 * est difficile a diagnostiquer. Le port 443, lui, est toujours ouvert.
 * <p>
 * Ce transport est donc prioritaire sur le SMTP lorsqu'une cle d'interface
 * est fournie. Le format de requete est celui de Resend ; il se transpose
 * sans difficulte a un autre fournisseur en changeant l'adresse et le
 * nommage des champs.
 */
@Slf4j
@Order(1)
@Component
public class HttpMailTransport implements MailTransport {

    private final HelpdeskProperties properties;
    private final ObjectMapper json;
    private final HttpClient client;

    public HttpMailTransport(HelpdeskProperties properties, ObjectMapper json) {
        this.properties = properties;
        this.json = json;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public boolean isAvailable() {
        HelpdeskProperties.Mail mail = properties.getMail();
        return notBlank(mail.getApiKey()) && notBlank(mail.getApiUrl());
    }

    @Override
    public void send(String to, String subject, String htmlBody) throws Exception {
        HelpdeskProperties.Mail mail = properties.getMail();

        // L'expediteur est compose ici et non dans le message : le nom
        // d'affichage vient de la configuration, jamais d'une saisie.
        String from = notBlank(mail.getFromName())
                ? mail.getFromName() + " <" + mail.getFrom() + ">"
                : mail.getFrom();

        byte[] corps = json.writeValueAsBytes(Map.of(
                "from", from,
                "to", List.of(to),
                "subject", subject,
                "html", htmlBody
        ));

        HttpRequest requete = HttpRequest.newBuilder(URI.create(mail.getApiUrl()))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + mail.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(corps))
                .build();

        HttpResponse<String> reponse =
                client.send(requete, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (reponse.statusCode() >= 300) {
            // Le corps de la reponse porte le motif exact du refus
            // (adresse non autorisee, domaine non verifie, quota). Le
            // journaliser economise une heure de recherche a l'exploitant.
            throw new IllegalStateException(
                    "reponse " + reponse.statusCode() + " : " + reponse.body());
        }
    }

    @Override
    public String name() {
        return "HTTPS";
    }

    private boolean notBlank(String valeur) {
        return valeur != null && !valeur.isBlank();
    }
}
