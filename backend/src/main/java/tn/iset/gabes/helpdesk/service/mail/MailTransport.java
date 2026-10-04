package tn.iset.gabes.helpdesk.service.mail;

/**
 * Moyen d'acheminement d'un courriel.
 * <p>
 * Deux implementations coexistent parce qu'aucune ne convient partout :
 * <ul>
 *   <li>{@link SmtpMailTransport} parle le protocole historique. Il
 *       fonctionne avec n'importe quel serveur de messagerie, mais exige
 *       un port sortant (587 ou 465) que beaucoup d'hebergeurs ferment
 *       pour limiter l'envoi de courriels non sollicites.</li>
 *   <li>{@link HttpMailTransport} passe par une requete HTTPS ordinaire,
 *       sur le port 443. Il traverse donc tous les reseaux, au prix d'une
 *       dependance au fournisseur choisi.</li>
 * </ul>
 * Separer l'acheminement de la redaction permet de changer de fournisseur
 * sans toucher au contenu des messages.
 */
public interface MailTransport {

    /** @return true si ce moyen est configure et utilisable */
    boolean isAvailable();

    /**
     * Achemine le message. Toute erreur est remontee : c'est a l'appelant
     * de decider qu'un courriel perdu ne doit pas faire echouer
     * l'enregistrement d'une panne.
     */
    void send(String to, String subject, String htmlBody) throws Exception;

    /** Nom court, pour les journaux. */
    String name();
}
