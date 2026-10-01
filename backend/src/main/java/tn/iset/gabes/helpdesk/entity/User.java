package tn.iset.gabes.helpdesk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.iset.gabes.helpdesk.entity.enums.Role;

/**
 * Compte utilisateur de l'application.
 * <p>
 * Une seule table accueille les trois roles. Le champ establishment
 * n'est renseigne que pour les comptes de role CLIENT : un administrateur
 * ou un technicien travaille pour la societe de maintenance et
 * n'appartient donc a aucun etablissement.
 * <p>
 * La table est nommee "users" car "user" est un mot reserve dans
 * plusieurs moteurs SQL.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    /** Empreinte BCrypt : le mot de passe en clair n'est jamais stocke. */
    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** Renseigne uniquement pour les comptes de role CLIENT. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "establishment_id")
    private Establishment establishment;

    /** Specialite du technicien, affichee au moment de l'affectation. */
    @Column(length = 100)
    private String speciality;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    /**
     * @return true si les donnees visibles par cet utilisateur doivent etre
     *         limitees a son propre etablissement
     */
    public boolean isTenantScoped() {
        return role == Role.CLIENT;
    }
}
