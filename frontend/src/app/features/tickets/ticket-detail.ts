import { DatePipe } from '@angular/common';
import { Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TicketDetail, UserResponse } from '../../core/models/api.models';
import { TicketStatus } from '../../core/models/enums';
import { AuthService } from '../../core/services/auth.service';
import { TicketService } from '../../core/services/ticket.service';
import { ToastService } from '../../core/services/toast.service';
import { UserService } from '../../core/services/user.service';
import { PriorityBadge, SlaIndicator, StatusBadge } from '../../shared/badges';

/**
 * Fiche detaillee d un ticket : informations, actions, echanges et
 * historique.
 *
 * Les actions proposees ne sont pas decidees ici : le serveur renvoie le
 * champ {@code allowedTransitions}, et l interface n affiche que ces
 * possibilites. Ajouter un etat au cycle de vie cote serveur suffit donc,
 * sans modifier ce composant.
 */
@Component({
  selector: 'app-ticket-detail',
  imports: [RouterLink, FormsModule, DatePipe, StatusBadge, PriorityBadge, SlaIndicator],
  templateUrl: './ticket-detail.html',
  styleUrl: './ticket-detail.scss',
})
export class TicketDetailPage {
  /** Identifiant issu de l URL, injecte par le routeur. */
  readonly id = input.required<string>();

  private readonly ticketService = inject(TicketService);
  private readonly userService = inject(UserService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  protected readonly ticket = signal<TicketDetail | null>(null);
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly technicians = signal<UserResponse[]>([]);

  // Champs des formulaires d action
  protected newComment = '';
  protected internalNote = false;
  protected resolutionNote = '';
  protected satisfaction: number | null = null;
  protected selectedTechnicianId: number | null = null;

  /** Statut en cours de confirmation, null si aucune action n est ouverte. */
  protected readonly pendingStatus = signal<TicketStatus | null>(null);

  constructor() {
    // input() est disponible des la construction grace a
    // withComponentInputBinding : on peut donc charger immediatement
    queueMicrotask(() => this.load());

    if (!this.auth.isClient()) {
      this.userService.technicians().subscribe((list) => this.technicians.set(list));
    }
  }

  protected load(): void {
    this.loading.set(true);
    this.ticketService.getById(Number(this.id())).subscribe({
      next: (ticket) => {
        this.ticket.set(ticket);
        this.selectedTechnicianId = ticket.assignedToId ?? null;
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        void this.router.navigate(['/tickets']);
      },
    });
  }

  // ------------------------------------------------------------- Affectation

  protected assign(): void {
    const technicianId = this.selectedTechnicianId;
    if (!technicianId || this.busy()) {
      return;
    }

    this.busy.set(true);
    this.ticketService.assign(Number(this.id()), technicianId).subscribe({
      next: (updated) => {
        this.ticket.set(updated);
        this.busy.set(false);
        this.toast.success(`Ticket affecte a ${updated.assignedToName}.`);
      },
      error: () => this.busy.set(false),
    });
  }

  /** Le technicien connecte prend le ticket en charge lui-meme. */
  protected takeOwnership(): void {
    const userId = this.auth.currentUser()?.id;
    if (!userId) {
      return;
    }
    this.selectedTechnicianId = userId;
    this.assign();
  }

  // --------------------------------------------------------- Cycle de vie

  /**
   * Ouvre la confirmation du changement de statut.
   * Les statuts RESOLU et CLOTURE demandent une saisie complementaire :
   * la description de la solution et la note de satisfaction.
   */
  protected openStatusChange(status: TicketStatus): void {
    this.pendingStatus.set(status);
    this.resolutionNote = '';
    this.satisfaction = null;
  }

  protected cancelStatusChange(): void {
    this.pendingStatus.set(null);
  }

  protected confirmStatusChange(): void {
    const status = this.pendingStatus();
    if (!status || this.busy()) {
      return;
    }

    if (status === 'RESOLVED' && this.resolutionNote.trim().length === 0) {
      this.toast.error('Merci de décrire la solution appliquee.');
      return;
    }

    this.busy.set(true);
    this.ticketService
      .changeStatus(Number(this.id()), {
        status,
        resolutionNote: this.resolutionNote.trim() || undefined,
        satisfactionRating: this.satisfaction ?? undefined,
      })
      .subscribe({
        next: (updated) => {
          this.ticket.set(updated);
          this.pendingStatus.set(null);
          this.busy.set(false);
          this.toast.success(`Le ticket est maintenant ${updated.statusLabel.toLowerCase()}.`);
        },
        error: () => this.busy.set(false),
      });
  }

  // ------------------------------------------------------------ Commentaires

  protected addComment(): void {
    const content = this.newComment.trim();
    if (!content || this.busy()) {
      return;
    }

    this.busy.set(true);
    this.ticketService.addComment(Number(this.id()), content, this.internalNote).subscribe({
      next: () => {
        this.newComment = '';
        this.internalNote = false;
        this.busy.set(false);
        // La fiche est rechargee pour recuperer aussi la nouvelle entree
        // d historique produite par le serveur
        this.load();
      },
      error: () => this.busy.set(false),
    });
  }

  // ------------------------------------------------------------- Utilitaires

  /** Libelle de l action proposee pour un statut donne. */
  protected actionLabel(status: TicketStatus): string {
    const labels: Record<TicketStatus, string> = {
      NEW: 'Remettre à nouveau',
      ASSIGNED: 'Marquer comme affecté',
      IN_PROGRESS: 'Démarrer le traitement',
      WAITING_CLIENT: 'Mettre en attente du client',
      RESOLVED: 'Déclarer résolu',
      CLOSED: 'Clôturer le ticket',
      CANCELLED: 'Annuler la demande',
    };
    return labels[status];
  }

  /**
   * Rang d'affichage des actions.
   *
   * Le serveur renvoie les transitions possibles dans l'ordre du cycle de
   * vie, ou l'annulation vient en tete. A l'ecran, cela placait l'abandon
   * du ticket au-dessus de sa resolution : le geste le plus visible etait
   * celui qu'on souhaite le moins. L'ordre d'affichage suit donc
   * l'intention, du plus constructif au plus definitif.
   */
  private static readonly RANGS: Record<TicketStatus, number> = {
    RESOLVED: 0,
    IN_PROGRESS: 1,
    ASSIGNED: 2,
    WAITING_CLIENT: 3,
    CLOSED: 4,
    NEW: 5,
    CANCELLED: 9,
  };

  protected actionsOrdonnees(transitions: TicketStatus[]): TicketStatus[] {
    return [...transitions].sort(
      (a, b) => TicketDetailPage.RANGS[a] - TicketDetailPage.RANGS[b],
    );
  }

  /** Les actions destructrices sont presentees differemment. */
  protected actionClass(status: TicketStatus): string {
    if (status === 'CANCELLED') return 'btn--danger-discret';
    if (status === 'RESOLVED' || status === 'CLOSED') return 'btn--success';
    return 'btn--secondary';
  }

  protected readonly ratingValues = [1, 2, 3, 4, 5];
}
