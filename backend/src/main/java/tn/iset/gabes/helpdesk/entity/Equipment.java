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
import tn.iset.gabes.helpdesk.entity.enums.EquipmentType;

import java.time.LocalDate;

/**
 * Materiel installe chez un client et couvert par le contrat de maintenance.
 * <p>
 * Chaque equipement possede un qrToken unique, encode dans une etiquette QR
 * collee sur la machine. En scannant cette etiquette, le gerant ouvre un
 * formulaire de ticket deja renseigne avec le materiel concerne, sans avoir
 * a le chercher dans une liste.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "equipments")
public class Equipment extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EquipmentType type;

    @Column(length = 60)
    private String brand;

    @Column(length = 60)
    private String model;

    @Column(name = "serial_number", length = 80)
    private String serialNumber;

    /** Emplacement dans l'etablissement : comptoir, cuisine, salle. */
    @Column(length = 100)
    private String location;

    /**
     * Identifiant public de l'equipement, encode dans le QR code.
     * Volontairement distinct de la cle primaire afin de ne pas exposer
     * la numerotation interne de la base.
     */
    @Column(name = "qr_token", nullable = false, unique = true, length = 36)
    private String qrToken;

    @Column(name = "installed_at")
    private LocalDate installedAt;

    @Column(name = "warranty_end")
    private LocalDate warrantyEnd;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false)
    private Establishment establishment;

    /**
     * @return true si le materiel est encore sous garantie a la date du jour
     */
    public boolean isUnderWarranty() {
        return warrantyEnd != null && !warrantyEnd.isBefore(LocalDate.now());
    }
}
