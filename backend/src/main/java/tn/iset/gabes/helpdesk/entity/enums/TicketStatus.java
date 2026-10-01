package tn.iset.gabes.helpdesk.entity.enums;

import java.util.Set;

/**
 * Etats du cycle de vie d'un ticket.
 * <p>
 * Chaque etat declare explicitement les etats vers lesquels la transition
 * est autorisee. Le service metier s'appuie sur cette definition pour
 * refuser toute transition incoherente (par exemple passer directement
 * de NEW a CLOSED sans resolution).
 */
public enum TicketStatus {

    CLOSED("Cloture", true),
    CANCELLED("Annule", true),
    RESOLVED("Resolu", false),
    WAITING_CLIENT("En attente du client", false),
    IN_PROGRESS("En cours de traitement", false),
    ASSIGNED("Affecte", false),
    NEW("Nouveau", false);

    private final String label;

    /** Un etat final arrete le decompte du SLA. */
    private final boolean finalState;

    TicketStatus(String label, boolean finalState) {
        this.label = label;
        this.finalState = finalState;
    }

    public String getLabel() {
        return label;
    }

    public boolean isFinalState() {
        return finalState;
    }

    /**
     * @return true si le ticket est encore considere comme ouvert.
     */
    public boolean isOpen() {
        return !finalState;
    }

    /**
     * Transitions autorisees depuis l'etat courant.
     */
    public Set<TicketStatus> allowedTransitions() {
        return switch (this) {
            case NEW -> Set.of(ASSIGNED, IN_PROGRESS, CANCELLED);
            case ASSIGNED -> Set.of(IN_PROGRESS, WAITING_CLIENT, CANCELLED);
            case IN_PROGRESS -> Set.of(WAITING_CLIENT, RESOLVED, CANCELLED);
            case WAITING_CLIENT -> Set.of(IN_PROGRESS, RESOLVED, CANCELLED);
            case RESOLVED -> Set.of(CLOSED, IN_PROGRESS);
            case CLOSED, CANCELLED -> Set.of();
        };
    }

    /**
     * @param target etat vise
     * @return true si le passage de l'etat courant vers {@code target} est permis
     */
    public boolean canMoveTo(TicketStatus target) {
        return allowedTransitions().contains(target);
    }
}
