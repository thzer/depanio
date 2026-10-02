package tn.iset.gabes.helpdesk.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Protection contre les essais de mot de passe en rafale.
 * <p>
 * Sans ce garde-fou, rien n'empeche un programme d'essayer des milliers
 * de mots de passe sur une adresse connue : aucun mot de passe choisi par
 * un humain n'y resiste longtemps. C'est la faille la plus simple a
 * exploiter sur une application exposee, et la plus simple a fermer.
 * <p>
 * Le comptage se fait sur deux cles a la fois :
 * <ul>
 *   <li>l'<b>adresse electronique</b> visee, pour proteger un compte
 *       precis meme si les tentatives viennent de partout ;</li>
 *   <li>l'<b>adresse IP</b> d'origine, pour arreter un balayage de
 *       plusieurs comptes depuis un meme point.</li>
 * </ul>
 * <p>
 * Le compteur vit en memoire. Avec plusieurs instances du service,
 * chacune aurait le sien, ce qui multiplierait d'autant le nombre
 * d'essais tolere. Pour le volume vise ici, une seule instance suffit ;
 * au-dela, ce compteur devrait etre partage.
 */
@Slf4j
@Service
public class LoginAttemptService {

    /** Essais infructueux toleres avant blocage. */
    private static final int MAX_ESSAIS = 6;

    /** Duree du blocage une fois la limite atteinte. */
    private static final Duration BLOCAGE = Duration.ofMinutes(15);

    /** Au-dela de ce delai sans echec, le compteur repart de zero. */
    private static final Duration OUBLI = Duration.ofMinutes(30);

    private final Map<String, Compteur> compteurs = new ConcurrentHashMap<>();

    /** Suivi des echecs pour une cle donnee. */
    private static final class Compteur {
        private final AtomicInteger echecs = new AtomicInteger(0);
        private volatile Instant dernierEchec = Instant.now();
        private volatile Instant blocageJusqua;
    }

    /**
     * @return true si cette cle est actuellement bloquee
     */
    public boolean estBloque(String cle) {
        Compteur c = compteurs.get(normaliser(cle));
        if (c == null || c.blocageJusqua == null) {
            return false;
        }
        if (Instant.now().isAfter(c.blocageJusqua)) {
            // Le blocage a expire : on repart proprement
            compteurs.remove(normaliser(cle));
            return false;
        }
        return true;
    }

    /** Minutes restantes avant la fin du blocage, pour informer l'utilisateur. */
    public long minutesRestantes(String cle) {
        Compteur c = compteurs.get(normaliser(cle));
        if (c == null || c.blocageJusqua == null) {
            return 0;
        }
        long minutes = Duration.between(Instant.now(), c.blocageJusqua).toMinutes() + 1;
        return Math.max(1, minutes);
    }

    /**
     * Enregistre un echec et declenche le blocage si la limite est atteinte.
     */
    public void echec(String cle) {
        String k = normaliser(cle);
        Compteur c = compteurs.computeIfAbsent(k, ignored -> new Compteur());

        // Un echec isole il y a longtemps ne doit pas compter avec celui-ci
        if (Duration.between(c.dernierEchec, Instant.now()).compareTo(OUBLI) > 0) {
            c.echecs.set(0);
        }

        c.dernierEchec = Instant.now();
        int total = c.echecs.incrementAndGet();

        if (total >= MAX_ESSAIS) {
            c.blocageJusqua = Instant.now().plus(BLOCAGE);
            log.warn("Trop d'echecs de connexion : acces suspendu {} minutes pour {}",
                    BLOCAGE.toMinutes(), masquer(k));
        }
    }

    /** Une connexion reussie efface l'historique de la cle. */
    public void reussite(String cle) {
        compteurs.remove(normaliser(cle));
    }

    /**
     * Purge periodique des compteurs devenus inutiles.
     * Sans elle, la table grossirait indefiniment au fil des tentatives,
     * ce qui finirait par consommer toute la memoire.
     */
    @Scheduled(fixedDelay = 10 * 60 * 1000)
    public void purger() {
        Instant limite = Instant.now().minus(OUBLI);
        compteurs.entrySet().removeIf(e -> {
            Compteur c = e.getValue();
            boolean blocageTermine = c.blocageJusqua == null
                    || Instant.now().isAfter(c.blocageJusqua);
            return blocageTermine && c.dernierEchec.isBefore(limite);
        });
    }

    private String normaliser(String cle) {
        return cle == null ? "" : cle.trim().toLowerCase();
    }

    /**
     * Masque la cle dans les journaux : une adresse complete y figurerait
     * en clair, consultable par quiconque accede aux journaux.
     */
    private String masquer(String cle) {
        if (cle.length() <= 4) {
            return "***";
        }
        return cle.substring(0, 2) + "***" + cle.substring(cle.length() - 2);
    }
}
