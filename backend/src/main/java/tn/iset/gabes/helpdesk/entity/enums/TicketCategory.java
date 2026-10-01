package tn.iset.gabes.helpdesk.entity.enums;

/**
 * Nature du probleme declare, utilisee pour l'affectation et les statistiques.
 */
public enum TicketCategory {

    HARDWARE_FAILURE("Panne materielle"),
    SOFTWARE_ISSUE("Probleme logiciel"),
    NETWORK("Reseau et connexion"),
    CONFIGURATION("Configuration et parametrage"),
    CONSUMABLE("Consommable"),
    TRAINING("Formation et assistance"),
    OTHER("Autre");

    private final String label;

    TicketCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
