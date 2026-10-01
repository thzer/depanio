package tn.iset.gabes.helpdesk.common.exception;

/**
 * Levee lorsqu'une operation viole une regle metier : transition de statut
 * interdite, adresse electronique deja utilisee, equipement appartenant a
 * un autre etablissement, etc.
 * <p>
 * Traduite en reponse HTTP 400 par le gestionnaire global d'exceptions.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
