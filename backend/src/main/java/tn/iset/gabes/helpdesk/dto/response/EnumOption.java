package tn.iset.gabes.helpdesk.dto.response;

/**
 * Couple valeur technique et libelle lisible, utilise pour alimenter les
 * listes deroulantes du frontend.
 * <p>
 * Les libelles restent ainsi definis a un seul endroit, cote serveur : ils
 * ne sont pas recopies dans le code Angular ou ils pourraient diverger.
 *
 * @param value nom de la constante, par exemple "HARDWARE_FAILURE"
 * @param label libelle affiche, par exemple "Panne materielle"
 */
public record EnumOption(String value, String label) {
}
