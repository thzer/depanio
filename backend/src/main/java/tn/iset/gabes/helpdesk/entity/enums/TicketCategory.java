package tn.iset.gabes.helpdesk.entity.enums;

/**
 * Nature du probleme declare, utilisee pour l'affectation et les statistiques.
 */
public enum TicketCategory {

    HARDWARE_FAILURE("Panne matérielle"),
    SOFTWARE_ISSUE("Problème logiciel"),
    NETWORK("Réseau et connexion"),
    CONFIGURATION("Configuration et paramétrage"),
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
