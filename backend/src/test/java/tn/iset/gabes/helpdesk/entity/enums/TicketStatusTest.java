package tn.iset.gabes.helpdesk.entity.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifie le cycle de vie d'un ticket.
 * <p>
 * Ces regles sont le coeur du systeme : elles garantissent qu'un ticket ne
 * peut pas etre place dans un etat incoherent, par exemple cloture sans
 * avoir ete resolu. Les verifier par des tests evite qu'une evolution
 * ulterieure ne les affaiblisse sans que personne ne s'en apercoive.
 */
@DisplayName("Cycle de vie d'un ticket")
class TicketStatusTest {

    @Test
    @DisplayName("un ticket nouveau ne peut pas etre cloture directement")
    void newTicketCannotBeClosedDirectly() {
        assertThat(TicketStatus.NEW.canMoveTo(TicketStatus.CLOSED)).isFalse();
    }

    @Test
    @DisplayName("un ticket nouveau peut etre affecte, demarre ou annule")
    void newTicketAllowsExpectedTransitions() {
        assertThat(TicketStatus.NEW.allowedTransitions())
                .containsExactlyInAnyOrder(
                        TicketStatus.ASSIGNED,
                        TicketStatus.IN_PROGRESS,
                        TicketStatus.CANCELLED);
    }

    @Test
    @DisplayName("la cloture n'est accessible que depuis l'etat resolu")
    void onlyResolvedCanBeClosed() {
        for (TicketStatus status : TicketStatus.values()) {
            boolean canClose = status.canMoveTo(TicketStatus.CLOSED);
            if (status == TicketStatus.RESOLVED) {
                assertThat(canClose)
                        .as("un ticket resolu doit pouvoir etre cloture")
                        .isTrue();
            } else {
                assertThat(canClose)
                        .as("un ticket %s ne doit pas pouvoir etre cloture", status)
                        .isFalse();
            }
        }
    }

    @Test
    @DisplayName("un ticket resolu peut etre reouvert si la solution est incomplete")
    void resolvedCanBeReopened() {
        assertThat(TicketStatus.RESOLVED.canMoveTo(TicketStatus.IN_PROGRESS)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"CLOSED", "CANCELLED"})
    @DisplayName("un etat final n'autorise aucune transition")
    void finalStatesAreTerminal(TicketStatus status) {
        assertThat(status.isFinalState()).isTrue();
        assertThat(status.isOpen()).isFalse();
        assertThat(status.allowedTransitions()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class,
            names = {"NEW", "ASSIGNED", "IN_PROGRESS", "WAITING_CLIENT", "RESOLVED"})
    @DisplayName("un ticket non clos est considere comme ouvert")
    void nonFinalStatesAreOpen(TicketStatus status) {
        assertThat(status.isOpen()).isTrue();
        assertThat(status.isFinalState()).isFalse();
    }

    @Test
    @DisplayName("aucun etat ne peut revenir a l'etat nouveau")
    void noTransitionBackToNew() {
        for (TicketStatus status : TicketStatus.values()) {
            assertThat(status.canMoveTo(TicketStatus.NEW))
                    .as("l'etat %s ne doit pas pouvoir revenir a NEW", status)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("un etat ne se declare jamais comme transition vers lui-meme")
    void noSelfTransition() {
        for (TicketStatus status : TicketStatus.values()) {
            assertThat(status.allowedTransitions())
                    .as("l'etat %s ne doit pas se contenir lui-meme", status)
                    .doesNotContain(status);
        }
    }

    @Test
    @DisplayName("chaque statut porte un libelle affichable")
    void everyStatusHasLabel() {
        for (TicketStatus status : TicketStatus.values()) {
            assertThat(status.getLabel()).isNotBlank();
        }
    }
}
