package tn.iset.gabes.helpdesk.entity.enums;

/**
 * Categories de materiel installe chez les clients.
 */
public enum EquipmentType {

    POS_TERMINAL("Caisse enregistreuse"),
    CARD_READER("Terminal de paiement"),
    COFFEE_MACHINE("Machine a cafe"),
    PRINTER("Imprimante tickets"),
    REFRIGERATION("Materiel frigorifique"),
    NETWORK_DEVICE("Equipement reseau"),
    SCALE("Balance"),
    TABLET("Tablette de commande"),
    OTHER("Autre");

    private final String label;

    EquipmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
