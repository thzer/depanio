package tn.iset.gabes.helpdesk.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.iset.gabes.helpdesk.entity.enums.EstablishmentType;

import java.util.ArrayList;
import java.util.List;

/**
 * Etablissement client : un cafe, un restaurant ou un salon de the.
 * <p>
 * Cette entite constitue l'unite de cloisonnement des donnees dans
 * l'architecture multi-locataire. Un utilisateur de role CLIENT ne peut
 * consulter que les tickets et les equipements rattaches a son
 * etablissement ; ce controle est applique dans la couche service.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "establishments")
public class Establishment extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstablishmentType type;

    @Column(length = 200)
    private String address;

    @Column(length = 60)
    private String city;

    @Column(length = 20)
    private String phone;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_email", length = 120)
    private String contactEmail;

    /** Numero du contrat de maintenance. */
    @Column(name = "contract_number", length = 40)
    private String contractNumber;

    /**
     * Un etablissement desactive ne peut plus ouvrir de ticket.
     * Un client n'est jamais supprime : son historique doit rester consultable.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "establishment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Equipment> equipments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "establishment")
    private List<User> users = new ArrayList<>();
}
