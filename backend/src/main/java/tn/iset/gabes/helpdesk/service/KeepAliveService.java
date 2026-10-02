package tn.iset.gabes.helpdesk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tn.iset.gabes.helpdesk.config.HelpdeskProperties;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Maintien du service en eveil.
 * <p>
 * Les offres d'hebergement gratuites mettent un service en veille apres
 * quelques minutes sans visite. Pour un site vitrine, c'est sans
 * importance. Ici, c'est un probleme reel : la surveillance des delais
 * contractuels est une tache de fond, et un service endormi ne constate
 * plus aucun depassement a l'heure. Un produit vendu sur la tenue des
 * delais ne peut pas dormir.
 * <p>
 * Une requete periodique sur sa propre adresse publique suffit a le
 * maintenir actif. Elle vise le point de supervision, le plus leger du
 * service, et ne touche jamais la base de donnees.
 * <p>
 * Ce comportement est desactive par defaut et n'a aucun sens sur un
 * hebergement qui ne met pas en veille : on l'active alors uniquement la
 * ou il est utile.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeepAliveService {

    private static final Duration DELAI = Duration.ofSeconds(20);

    private final HelpdeskProperties properties;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(DELAI)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Scheduled(cron = "${helpdesk.keep-alive.cron:0 */10 * * * *}")
    public void maintenirEveille() {
        if (!properties.getKeepAlive().isEnabled()) {
            return;
        }

        String adresse = properties.getFrontendUrl();
        if (adresse == null || adresse.isBlank() || adresse.contains("localhost")) {
            return;
        }

        try {
            HttpRequest requete = HttpRequest.newBuilder()
                    .uri(URI.create(adresse + "/actuator/health"))
                    .timeout(DELAI)
                    .GET()
                    .build();

            HttpResponse<Void> reponse =
                    client.send(requete, HttpResponse.BodyHandlers.discarding());

            log.debug("Requete d'eveil : {}", reponse.statusCode());

        } catch (InterruptedException ex) {
            // L'arret du service interrompt l'attente : on restaure le
            // drapeau pour que l'arret se poursuive normalement.
            Thread.currentThread().interrupt();

        } catch (Exception ex) {
            // Un echec est sans consequence : la tentative suivante aura
            // lieu dix minutes plus tard.
            log.debug("Requete d'eveil sans reponse : {}", ex.getMessage());
        }
    }
}
