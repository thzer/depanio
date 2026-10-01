package tn.iset.gabes.helpdesk.common.exception;

/**
 * Levee lorsqu'une ressource demandee n'existe pas.
 * Traduite en reponse HTTP 404 par le gestionnaire global d'exceptions.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * @param resource nom de la ressource, par exemple "Ticket"
     * @param id       identifiant recherche
     */
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " introuvable (identifiant : " + id + ")");
    }
}
