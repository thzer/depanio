package tn.iset.gabes.helpdesk.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tn.iset.gabes.helpdesk.entity.enums.Role;

/**
 * Creation d'un compte utilisateur par un administrateur.
 */
public record CreateUserRequest(

        @NotBlank(message = "Le nom complet est obligatoire.")
        @Size(max = 120, message = "Le nom ne peut pas depasser 120 caracteres.")
        String fullName,

        @NotBlank(message = "L'adresse electronique est obligatoire.")
        @Email(message = "Le format de l'adresse electronique est invalide.")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 10, message = "Le mot de passe doit contenir au moins 10 caracteres.")
        String password,

        @Pattern(regexp = "^$|^[0-9+ ]{8,20}$",
                 message = "Le numero de telephone est invalide.")
        String phone,

        @NotNull(message = "Le role est obligatoire.")
        Role role,

        /** Obligatoire pour le role CLIENT, ignore pour les autres roles. */
        Long establishmentId,

        /** Specialite du technicien. */
        String speciality
) {
}
