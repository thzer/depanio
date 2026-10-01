package tn.iset.gabes.helpdesk.mapper;

import org.springframework.stereotype.Component;
import tn.iset.gabes.helpdesk.dto.response.UserResponse;
import tn.iset.gabes.helpdesk.entity.User;

/**
 * Conversion des entites {@link User} vers leur representation exposee.
 * <p>
 * La conversion est ecrite a la main plutot que generee : elle reste ainsi
 * lisible et explicite sur les champs volontairement omis, en premier lieu
 * l'empreinte du mot de passe.
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getRole().getLabel(),
                user.getSpeciality(),
                user.getEstablishment() != null ? user.getEstablishment().getId() : null,
                user.getEstablishment() != null ? user.getEstablishment().getName() : null,
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
