package tn.iset.gabes.helpdesk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Demande de demonstration laissee par un visiteur.
 * <p>
 * Elle est enregistree plutot qu'envoyee par courriel : une adresse
 * {@code mailto:} ne fonctionne que si le visiteur a un logiciel de
 * messagerie configure, ce qui est rarement le cas. Un prospect qui
 * clique et ne voit rien se passer est un prospect perdu.
 * <p>
 * Conserver la demande en base garantit en outre qu'elle survit a une
 * panne de messagerie, et qu'elle reste consultable tant qu'elle n'a pas
 * ete traitee.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "demo_requests",
        indexes = @Index(name = "idx_demo_request_handled", columnList = "handled, created_at")
)
public class DemoRequest extends BaseEntity {

    @Column(name = "contact_name", nullable = false, length = 120)
    private String contactName;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "company_name", length = 150)
    private String companyName;

    /** Nombre d'etablissements suivis, utile pour cadrer la proposition. */
    @Column(name = "establishment_count", length = 40)
    private String establishmentCount;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String message;

    /** Une demande traitee disparait de la liste des demandes en attente. */
    @Builder.Default
    @Column(nullable = false)
    private boolean handled = false;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    /** Marque la demande comme traitee et horodate l'action. */
    public void markHandled() {
        if (!handled) {
            this.handled = true;
            this.handledAt = LocalDateTime.now();
        }
    }
}
