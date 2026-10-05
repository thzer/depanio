package tn.iset.gabes.helpdesk.entity.enums;

/**
 * Motifs d'envoi d'une notification a un utilisateur.
 */
public enum NotificationType {

    TICKET_CREATED("Nouveau ticket"),
    TICKET_ASSIGNED("Ticket affecté"),
    TICKET_UPDATED("Ticket mis à jour"),
    TICKET_RESOLVED("Ticket résolu"),
    TICKET_CLOSED("Ticket clôturé"),
    NEW_COMMENT("Nouveau commentaire"),
    SLA_WARNING("Délai bientôt dépassé"),
    SLA_BREACHED("Délai dépassé");

    private final String label;

    NotificationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
