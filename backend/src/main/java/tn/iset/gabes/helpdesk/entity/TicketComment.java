package tn.iset.gabes.helpdesk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Message echange au sujet d'un ticket.
 * <p>
 * Le drapeau {@code internalNote} distingue les echanges visibles par le
 * client des notes reservees a l'equipe technique. Les notes internes sont
 * filtrees cote service avant d'etre renvoyees a un utilisateur CLIENT.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ticket_comments")
public class TicketComment extends BaseEntity {

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** true : note interne a l'equipe technique, invisible pour le client. */
    @Builder.Default
    @Column(name = "internal_note", nullable = false)
    private boolean internalNote = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;
}
