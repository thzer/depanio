import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Page, TicketSearchParams, TicketSummary } from '../../core/models/api.models';
import { EnumOption, Priority, TicketStatus } from '../../core/models/enums';
import { AuthService } from '../../core/services/auth.service';
import { ReferenceService } from '../../core/services/reference.service';
import { TicketService } from '../../core/services/ticket.service';
import { PriorityBadge, SlaIndicator, StatusBadge } from '../../shared/badges';

/**
 * Liste des tickets, avec filtres et pagination.
 *
 * Le filtrage est effectue par le serveur et non dans le navigateur :
 * charger plusieurs milliers de tickets pour les trier localement ne
 * tiendrait pas a l usage. La recherche est volontairement declenchee par
 * un bouton plutot qu a chaque frappe, afin de ne pas multiplier les
 * appels inutiles.
 */
@Component({
  selector: 'app-ticket-list',
  imports: [RouterLink, FormsModule, DatePipe, StatusBadge, PriorityBadge, SlaIndicator],
  templateUrl: './ticket-list.html',
  styles: `
    .filters {
      display: grid;
      grid-template-columns: 2fr 1fr 1fr auto;
      gap: var(--sp-4);
      align-items: end;
    }

    .filters__check {
      display: flex;
      align-items: center;
      gap: var(--sp-2);
      font-size: 0.85rem;
      white-space: nowrap;
      padding-bottom: 9px;
    }

    .filters__check input {
      width: auto;
    }

    .filters__actions {
      display: flex;
      gap: var(--sp-2);
      grid-column: 1 / -1;
    }

    @media (max-width: 900px) {
      .filters {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class TicketListPage {
  private readonly ticketService = inject(TicketService);
  private readonly referenceService = inject(ReferenceService);
  protected readonly auth = inject(AuthService);

  protected readonly page = signal<Page<TicketSummary> | null>(null);
  protected readonly loading = signal(true);

  protected readonly statuses = signal<EnumOption[]>([]);
  protected readonly priorities = signal<EnumOption[]>([]);

  // Criteres de recherche lies aux champs du formulaire
  protected search = '';
  protected status: TicketStatus | '' = '';
  protected priority: Priority | '' = '';
  protected onlyOpen = false;
  protected currentPage = 0;

  protected readonly pageSize = 20;

  constructor() {
    this.referenceService.all().subscribe((reference) => {
      this.statuses.set(reference.statuses);
      this.priorities.set(reference.priorities);
    });
    this.load();
  }

  protected load(): void {
    this.loading.set(true);

    const params: TicketSearchParams = {
      page: this.currentPage,
      size: this.pageSize,
      sort: 'createdAt,desc',
      onlyOpen: this.onlyOpen,
    };
    if (this.search.trim()) params.search = this.search.trim();
    if (this.status) params.status = this.status;
    if (this.priority) params.priority = this.priority;

    this.ticketService.search(params).subscribe({
      next: (result) => {
        this.page.set(result);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  /** Relance la recherche depuis la premiere page. */
  protected applyFilters(): void {
    this.currentPage = 0;
    this.load();
  }

  protected resetFilters(): void {
    this.search = '';
    this.status = '';
    this.priority = '';
    this.onlyOpen = false;
    this.applyFilters();
  }

  protected goToPage(index: number): void {
    this.currentPage = index;
    this.load();
  }
}
