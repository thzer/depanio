package tn.iset.gabes.helpdesk.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Formulaire de demande de demonstration, rempli par un visiteur.
 */
public record DemoRequestForm(

        @NotBlank(message = "Votre nom est obligatoire.")
        @Size(max = 120, message = "Le nom ne peut pas depasser 120 caracteres.")
        String contactName,

        @NotBlank(message = "Votre adresse electronique est obligatoire.")
        @Email(message = "Le format de l adresse electronique est invalide.")
        @Size(max = 120)
        String email,

        @Size(max = 30, message = "Le numero de telephone est trop long.")
        String phone,

        @Size(max = 150, message = "Le nom de la societe est trop long.")
        String companyName,

        @Size(max = 40)
        String establishmentCount,

        @Size(max = 2000, message = "Le message ne peut pas depasser 2000 caracteres.")
        String message,

        /**
         * Champ piege, invisible pour un visiteur et laisse vide par lui.
         * Les robots remplissent tous les champs d un formulaire : une
         * valeur ici trahit donc un envoi automatise.
         */
        String website
) {
}
