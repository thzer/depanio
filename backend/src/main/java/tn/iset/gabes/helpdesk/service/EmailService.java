package tn.iset.gabes.helpdesk.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tn.iset.gabes.helpdesk.config.HelpdeskProperties;
import tn.iset.gabes.helpdesk.entity.Ticket;
import tn.iset.gabes.helpdesk.entity.User;
import tn.iset.gabes.helpdesk.entity.enums.NotificationType;
import tn.iset.gabes.helpdesk.service.mail.MailTransport;

import java.util.List;

/**
 * Envoi des courriels de notification.
 * <p>
 * Les notifications affichees dans l'application ne suffisent pas a une
 * societe de maintenance : un technicien en deplacement n'a pas le
 * navigateur ouvert. Le courriel est le seul canal qui le joigne sans
 * rien installer sur son telephone ni rien payer.
 * <p>
 * Trois precautions guident cette classe :
 * <ul>
 *   <li><b>Facultatif</b> : sans serveur configure, rien n'est envoye et
 *       l'application fonctionne normalement.</li>
 *   <li><b>Asynchrone</b> : l'utilisateur qui cree un ticket n'attend pas
 *       que le courriel parte.</li>
 *   <li><b>Sans consequence en cas d'echec</b> : un serveur de messagerie
 *       indisponible ne doit jamais faire echouer l'enregistrement d'une
 *       panne. L'incident est journalise, rien de plus.</li>
 * </ul>
 */
@Slf4j
@Service
public class EmailService {

    /**
     * Motifs qui justifient un courriel.
     * <p>
     * Volontairement restreint : un courriel pour chaque commentaire
     * finirait en regle de filtrage dans la boite du technicien, et les
     * alertes vraiment urgentes seraient perdues avec le reste.
     */
    private static final java.util.Set<NotificationType> MOTIFS_COURRIEL = java.util.Set.of(
            NotificationType.TICKET_ASSIGNED,
            NotificationType.SLA_WARNING,
            NotificationType.SLA_BREACHED,
            NotificationType.TICKET_CREATED
    );

    private final List<MailTransport> transports;
    private final HelpdeskProperties properties;

    public EmailService(List<MailTransport> transports,
                        HelpdeskProperties properties) {
        // La liste est triee par Spring selon l'annotation @Order : le
        // premier moyen utilisable est retenu. Cette classe redige le
        // message, elle n'a pas a savoir comment il circule.
        this.transports = transports;
        this.properties = properties;
    }

    /**
     * @return true si un courriel doit partir pour ce motif
     */
    public boolean concerne(NotificationType type) {
        return isConfigured() && MOTIFS_COURRIEL.contains(type);
    }

    /**
     * Envoie la notification par courriel.
     *
     * @param destinataire utilisateur concerne
     * @param type         motif de la notification
     * @param message      texte deja redige pour l'application
     * @param ticket       ticket concerne, peut etre null
     */
    @Async
    public void envoyer(User destinataire, NotificationType type, String message, Ticket ticket) {
        if (!concerne(type) || destinataire == null || isBlank(destinataire.getEmail())) {
            return;
        }

        MailTransport transport = transportRetenu();
        if (transport == null) {
            return;
        }

        try {
            transport.send(destinataire.getEmail(),
                    sujet(type, ticket),
                    corps(destinataire, type, message, ticket));

            log.info("Courriel envoye a {} par {} ({})",
                    destinataire.getEmail(), transport.name(), type);

        } catch (Exception ex) {
            // Un serveur de messagerie en panne ne doit pas empecher
            // d'enregistrer une intervention : on journalise et on continue.
            log.warn("Envoi du courriel impossible vers {} par {} : {}",
                    destinataire.getEmail(), transport.name(), ex.getMessage());
        }
    }

    /**
     * @return le premier moyen d'acheminement utilisable, null s'il n'y en
     *         a aucun ou si les courriels sont desactives
     */
    private MailTransport transportRetenu() {
        if (!properties.getMail().isEnabled() || isBlank(properties.getMail().getFrom())) {
            return null;
        }
        return transports.stream()
                .filter(MailTransport::isAvailable)
                .findFirst()
                .orElse(null);
    }

    private boolean isConfigured() {
        return transportRetenu() != null;
    }

    private String sujet(NotificationType type, Ticket ticket) {
        // La reference est generee par l'application, pas saisie :
        // elle ne peut pas contenir de retour a la ligne, qui permettrait
        // d'ajouter des en-tetes au message.
        String reference = ticket != null ? ticket.getReference() : "";
        return switch (type) {
            case TICKET_ASSIGNED -> "[" + reference + "] Une intervention vous est confiee";
            case SLA_WARNING -> "[" + reference + "] L'echeance approche";
            case SLA_BREACHED -> "[" + reference + "] Delai depasse";
            case TICKET_CREATED -> "[" + reference + "] Nouvelle panne declaree";
            default -> "[" + reference + "] " + type.getLabel();
        };
    }

    /**
     * Compose un message lisible sur telephone : une phrase utile, les
     * informations qui permettent de decider, puis un lien direct.
     * <p>
     * La mise en forme est volontairement sobre et en ligne : les clients
     * de messagerie ignorent les feuilles de style externes.
     */
    private String corps(User destinataire, NotificationType type, String message, Ticket ticket) {
        String lien = properties.getFrontendUrl()
                + (ticket != null ? "/tickets/" + ticket.getId() : "/tickets");

        StringBuilder details = new StringBuilder();
        if (ticket != null) {
            ajouter(details, "Reference", ticket.getReference());
            ajouter(details, "Objet", ticket.getTitle());
            ajouter(details, "Urgence", ticket.getPriority().getLabel());
            if (ticket.getEstablishment() != null) {
                ajouter(details, "Etablissement", ticket.getEstablishment().getName()
                        + (isBlank(ticket.getEstablishment().getCity()) ? ""
                        : " — " + ticket.getEstablishment().getCity()));
                ajouter(details, "Telephone", ticket.getEstablishment().getPhone());
            }
            if (ticket.getEquipment() != null) {
                ajouter(details, "Materiel", ticket.getEquipment().getName()
                        + (isBlank(ticket.getEquipment().getLocation()) ? ""
                        : " (" + ticket.getEquipment().getLocation() + ")"));
            }
            if (ticket.getSlaDueAt() != null) {
                ajouter(details, "A resoudre avant", ticket.getSlaDueAt().toString()
                        .replace('T', ' ').substring(0, 16));
            }
        }

        boolean urgent = type == NotificationType.SLA_BREACHED
                || type == NotificationType.SLA_WARNING;
        String accent = urgent ? "#b42318" : "#0f766e";

        return """
                <div style="font-family:system-ui,-apple-system,'Segoe UI',sans-serif;\
                max-width:560px;margin:0 auto;color:#212b3f;line-height:1.55">
                  <div style="padding:20px 0;border-bottom:3px solid %s">
                    <span style="font-size:19px;font-weight:700;color:%s">Depanio</span>
                  </div>

                  <p style="margin:24px 0 8px">Bonjour %s,</p>
                  <p style="margin:0 0 20px;font-size:16px">%s</p>

                  <table style="width:100%%;border-collapse:collapse;font-size:14px">%s</table>

                  <p style="margin:28px 0">
                    <a href="%s" style="display:inline-block;padding:11px 20px;\
                background:%s;color:#fff;text-decoration:none;border-radius:7px;\
                font-weight:600">Ouvrir le ticket</a>
                  </p>

                  <p style="margin:24px 0 0;padding-top:16px;border-top:1px solid #e2e7ee;\
                font-size:12px;color:#6b7892">
                    Message automatique de Depanio. Inutile d'y repondre.
                  </p>
                </div>
                """.formatted(accent, accent,
                echapper(destinataire.getFullName()), echapper(message),
                // Le tableau est deja construit avec des valeurs echappees
                details.toString(), echapper(lien), accent);
    }

    private void ajouter(StringBuilder table, String libelle, String valeur) {
        if (isBlank(valeur)) {
            return;
        }
        table.append("""
                <tr>
                  <td style="padding:5px 12px 5px 0;color:#6b7892;white-space:nowrap;\
                vertical-align:top">%s</td>
                  <td style="padding:5px 0;font-weight:600">%s</td>
                </tr>
                """.formatted(echapper(libelle), echapper(valeur)));
    }

    /**
     * Neutralise les caracteres qui ont un sens en HTML.
     * <p>
     * Le titre d'un ticket, le nom d'un etablissement ou celui d'un
     * materiel sont saisis par des utilisateurs. Inseres tels quels dans
     * un courriel en HTML, ils permettraient d'y glisser des balises, et
     * donc de fabriquer un faux message a l'interieur d'un courriel qui
     * parait venir de l'application : une invitation a cliquer sur un lien
     * frauduleux, par exemple.
     */
    private String echapper(String valeur) {
        if (valeur == null) {
            return "";
        }
        return valeur
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
