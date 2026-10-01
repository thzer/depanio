package tn.iset.gabes.helpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Ajout d'un message a un ticket.
 */
public record AddCommentRequest(

        @NotBlank(message = "Le contenu du commentaire est obligatoire.")
        String content,

        /**
         * true pour une note interne, invisible par le client.
         * Le service refuse cette valeur si l'auteur est un CLIENT.
         */
        boolean internalNote
) {
}
