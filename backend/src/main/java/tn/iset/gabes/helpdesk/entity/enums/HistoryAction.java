package tn.iset.gabes.helpdesk.entity.enums;

/**
 * Types d'evenements conserves dans l'historique d'un ticket.
 * Cet historique constitue la piste d'audit du systeme.
 */
public enum HistoryAction {

    CREATED("Création du ticket"),
    STATUS_CHANGED("Changement de statut"),
    ASSIGNED("Affectation"),
    UNASSIGNED("Désaffectation"),
    PRIORITY_CHANGED("Changement de priorité"),
    COMMENT_ADDED("Ajout d’un commentaire"),
    RESOLVED("Résolution"),
    CLOSED("Clôturé"),
    REOPENED("Réouverture"),
    SLA_BREACHED("Dépassement du délai contractuel");

    private final String label;

    HistoryAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
