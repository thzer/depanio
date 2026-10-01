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
import tn.iset.gabes.helpdesk.entity.enums.HistoryAction;

/**
 * Piste d'audit d'un ticket : qui a fait quoi et quand.
 * <p>
 * Chaque evenement est conserve de maniere immuable. Cette trace permet de
 * justifier le respect ou le depassement d'un delai contractuel et de
 * reconstituer le deroulement complet d'une intervention.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ticket_history")
public class TicketHistory extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HistoryAction action;

    /** Valeur avant modification, sous forme lisible. */
    @Column(name = "old_value", length = 150)
    private String oldValue;

    /** Valeur apres modification, sous forme lisible. */
    @Column(name = "new_value", length = 150)
    private String newValue;

    /** Precision facultative sur l'evenement. */
    @Column(length = 255)
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    /**
     * Auteur de l'action. Null lorsque l'evenement est produit par le
     * systeme lui-meme, par exemple le depassement automatique d'un SLA.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;
}
