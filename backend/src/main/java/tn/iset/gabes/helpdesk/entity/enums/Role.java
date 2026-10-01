package tn.iset.gabes.helpdesk.entity.enums;

/**
 * Roles disponibles dans l'application.
 * Le prefixe "ROLE_" attendu par Spring Security est ajoute au moment
 * de la construction des autorites (voir {@code UserPrincipal}).
 */
public enum Role {

    /** Administrateur de la societe de maintenance : acces total. */
    ADMIN("Administrateur"),

    /** Technicien : traite les tickets qui lui sont affectes. */
    TECHNICIAN("Technicien"),

    /** Client : gerant d'un cafe ou d'un restaurant, declare les pannes. */
    CLIENT("Client");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
