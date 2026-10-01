package tn.iset.gabes.helpdesk.entity.enums;

/**
 * Types d'evenements conserves dans l'historique d'un ticket.
 * Cet historique constitue la piste d'audit du systeme.
 */
public enum HistoryAction {

    CREATED("Creation du ticket"),
    STATUS_CHANGED("Changement de statut"),
    ASSIGNED("Affectation"),
    UNASSIGNED("Desaffectation"),
    PRIORITY_CHANGED("Changement de priorite"),
    COMMENT_ADDED("Ajout d'un commentaire"),
    RESOLVED("Resolution"),
    CLOSED("Cloture"),
    REOPENED("Reouverture"),
    SLA_BREACHED("Depassement du delai SLA");

    private final String label;

    HistoryAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
