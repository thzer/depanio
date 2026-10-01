package tn.iset.gabes.helpdesk.entity.enums;

/**
 * Nature de l'etablissement client.
 */
public enum EstablishmentType {

    CAFE("Cafe"),
    RESTAURANT("Restaurant"),
    TEA_HOUSE("Salon de the"),
    FAST_FOOD("Fast-food"),
    PASTRY("Patisserie"),
    OTHER("Autre");

    private final String label;

    EstablishmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
