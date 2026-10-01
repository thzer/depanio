package tn.iset.gabes.helpdesk.dto.response;

import tn.iset.gabes.helpdesk.entity.enums.Role;

import java.time.LocalDateTime;

/**
 * Representation publique d'un utilisateur.
 * L'empreinte du mot de passe n'y figure jamais.
 */
public record UserResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        Role role,
        String roleLabel,
        String speciality,
        Long establishmentId,
        String establishmentName,
        boolean active,
        LocalDateTime createdAt
) {
}
