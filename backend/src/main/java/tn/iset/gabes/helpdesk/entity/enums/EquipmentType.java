package tn.iset.gabes.helpdesk.entity.enums;

/**
 * Categories de materiel installe chez les clients.
 */
public enum EquipmentType {

    POS_TERMINAL("Caisse enregistreuse"),
    CARD_READER("Terminal de paiement"),
    COFFEE_MACHINE("Machine à café"),
    PRINTER("Imprimante tickets"),
    REFRIGERATION("Matériel frigorifique"),
    NETWORK_DEVICE("Équipement réseau"),
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
