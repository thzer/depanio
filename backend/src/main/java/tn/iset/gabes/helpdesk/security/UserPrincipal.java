package tn.iset.gabes.helpdesk.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import tn.iset.gabes.helpdesk.entity.User;
import tn.iset.gabes.helpdesk.entity.enums.Role;

import java.util.Collection;
import java.util.List;

/**
 * Utilisateur authentifie tel que Spring Security le manipule.
 * <p>
 * Cette classe transporte, en plus des informations attendues par le
 * framework, l'identifiant de l'utilisateur et celui de son etablissement.
 * Les services peuvent ainsi appliquer le cloisonnement multi-locataire
 * sans relire l'utilisateur en base a chaque requete.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String fullName;
    private final Role role;

    /** Identifiant de l'etablissement, null pour un administrateur ou un technicien. */
    private final Long establishmentId;

    private final boolean active;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.fullName = user.getFullName();
        this.role = user.getRole();
        this.establishmentId = user.getEstablishment() != null
                ? user.getEstablishment().getId()
                : null;
        this.active = user.isActive();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security attend le prefixe ROLE_ pour les controles hasRole(...)
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isTechnician() {
        return role == Role.TECHNICIAN;
    }

    public boolean isClient() {
        return role == Role.CLIENT;
    }
}
