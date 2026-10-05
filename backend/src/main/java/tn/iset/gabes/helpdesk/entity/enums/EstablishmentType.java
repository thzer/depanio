package tn.iset.gabes.helpdesk.entity.enums;

/**
 * Nature de l'etablissement client.
 */
public enum EstablishmentType {

    CAFE("Café"),
    RESTAURANT("Restaurant"),
    TEA_HOUSE("Salon de thé"),
    FAST_FOOD("Fast-food"),
    PASTRY("Pâtisserie"),
    OTHER("Autre");

    private final String label;

    EstablishmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
