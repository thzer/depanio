package tn.iset.gabes.helpdesk.entity.enums;

/**
 * Niveaux d'urgence d'un ticket.
 * <p>
 * Le niveau choisi determine le delai de resolution contractuel (SLA).
 * Les durees reelles sont configurables dans {@code application.yml}
 * sous la cle {@code helpdesk.sla}, elles ne sont donc pas codees ici.
 */
public enum Priority {

    /** Activite totalement bloquee : caisse hors service en pleine affluence. */
    CRITICAL("Critique", 1),

    /** Fonction importante indisponible mais contournable. */
    HIGH("Haute", 2),

    /** Gene moderee, l'activite continue. */
    MEDIUM("Moyenne", 3),

    /** Demande sans urgence : evolution, question, formation. */
    LOW("Basse", 4);

    private final String label;

    /** Poids utilise pour le tri : 1 = a traiter en premier. */
    private final int weight;

    Priority(String label, int weight) {
        this.label = label;
        this.weight = weight;
    }

    public String getLabel() {
        return label;
    }

    public int getWeight() {
        return weight;
    }
}
