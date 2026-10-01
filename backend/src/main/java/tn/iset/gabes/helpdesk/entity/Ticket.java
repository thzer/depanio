package tn.iset.gabes.helpdesk.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.iset.gabes.helpdesk.entity.enums.Priority;
import tn.iset.gabes.helpdesk.entity.enums.TicketCategory;
import tn.iset.gabes.helpdesk.entity.enums.TicketStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ticket d'assistance : entite centrale de l'application.
 * <p>
 * Un ticket represente une demande d'intervention declaree par un
 * etablissement client. Il porte son propre cycle de vie (voir
 * {@link TicketStatus}), un delai de resolution contractuel calcule a la
 * creation a partir de la priorite, et la trace complete de son traitement
 * via ses commentaires et son historique.
 * <p>
 * Des index sont declares sur les colonnes les plus filtrees afin que les
 * listes et le tableau de bord restent rapides lorsque le volume augmente.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "tickets",
        indexes = {
                @Index(name = "idx_ticket_status", columnList = "status"),
                @Index(name = "idx_ticket_priority", columnList = "priority"),
                @Index(name = "idx_ticket_establishment", columnList = "establishment_id"),
                @Index(name = "idx_ticket_assignee", columnList = "assigned_to_id"),
                @Index(name = "idx_ticket_sla", columnList = "sla_due_at")
        }
)
public class Ticket extends BaseEntity {

    /**
     * Reference lisible communiquee au client, au format TK-2026-0001.
     * Elle est generee par le service a la creation du ticket.
     */
    @Column(nullable = false, unique = true, length = 20)
    private String reference;

    @Column(nullable = false, length = 150)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketStatus status = TicketStatus.NEW;

    // ----- Rattachements -----

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false)
    private Establishment establishment;

    /** Materiel concerne. Null pour une demande qui ne vise aucun equipement precis. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id")
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    /** Technicien en charge. Null tant que le ticket n'est pas affecte. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    // ----- Suivi du delai contractuel (SLA) -----

    /** Echeance de resolution calculee a la creation selon la priorite. */
    @Column(name = "sla_due_at")
    private LocalDateTime slaDueAt;

    /** Positionne par la tache planifiee lorsque l'echeance est depassee. */
    @Builder.Default
    @Column(name = "sla_breached", nullable = false)
    private boolean slaBreached = false;

    // ----- Horodatages du traitement -----

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /** Description de la solution appliquee, saisie par le technicien. */
    @Lob
    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    /** Note de satisfaction donnee par le client a la cloture, de 1 a 5. */
    @Column(name = "satisfaction_rating")
    private Integer satisfactionRating;

    // ----- Collections -----

    @Builder.Default
    @OrderBy("createdAt ASC")
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketComment> comments = new ArrayList<>();

    @Builder.Default
    @OrderBy("createdAt DESC")
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketHistory> history = new ArrayList<>();

    // ----- Methodes metier -----

    /**
     * Le decompte du delai contractuel est-il encore en cours ?
     * <p>
     * L'engagement porte sur le temps de resolution : il s'arrete donc des
     * que le ticket est resolu, et non a sa cloture. Un ticket resolu qui
     * attend la confirmation du client n'accumule plus de retard.
     */
    private boolean isSlaRunning() {
        return slaDueAt != null && resolvedAt == null && status.isOpen();
    }

    /**
     * @return true si le delai contractuel est depasse alors que le
     *         decompte est toujours en cours
     */
    public boolean isOverdue() {
        return isSlaRunning() && LocalDateTime.now().isAfter(slaDueAt);
    }

    /**
     * Temps restant avant l'echeance contractuelle.
     *
     * @return la duree restante, negative si l'echeance est depassee, ou
     *         null si le decompte est arrete ou n'a jamais commence
     */
    public Duration getTimeRemaining() {
        if (!isSlaRunning()) {
            return null;
        }
        return Duration.between(LocalDateTime.now(), slaDueAt);
    }

    /**
     * Duree totale de traitement, de la creation a la resolution.
     *
     * @return la duree de resolution, ou null si le ticket n'est pas resolu
     */
    public Duration getResolutionTime() {
        if (getCreatedAt() == null || resolvedAt == null) {
            return null;
        }
        return Duration.between(getCreatedAt(), resolvedAt);
    }

    /** Ajoute un commentaire en maintenant la coherence des deux cotes de la relation. */
    public void addComment(TicketComment comment) {
        comments.add(comment);
        comment.setTicket(this);
    }

    /** Ajoute une entree d'historique en maintenant la coherence de la relation. */
    public void addHistoryEntry(TicketHistory entry) {
        history.add(entry);
        entry.setTicket(this);
    }
}
