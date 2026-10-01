package tn.iset.gabes.helpdesk.entity.enums;

/**
 * Motifs d'envoi d'une notification a un utilisateur.
 */
public enum NotificationType {

    TICKET_CREATED("Nouveau ticket"),
    TICKET_ASSIGNED("Ticket affecte"),
    TICKET_UPDATED("Ticket mis a jour"),
    TICKET_RESOLVED("Ticket resolu"),
    TICKET_CLOSED("Ticket cloture"),
    NEW_COMMENT("Nouveau commentaire"),
    SLA_WARNING("Delai bientot depasse"),
    SLA_BREACHED("Delai depasse");

    private final String label;

    NotificationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
