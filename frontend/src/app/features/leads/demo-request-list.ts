import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DemoRequest, Page } from '../../core/models/api.models';
import { DemoRequestService } from '../../core/services/demo-request.service';
import { ToastService } from '../../core/services/toast.service';
import { Icon } from '../../shared/icon';

/**
 * Demandes de démonstration recues depuis la page commerciale.
 *
 * Elles arrivent ici plutot que dans une boite de courriel : rien ne se
 * perd dans un dossier indesirables, et chaque demande reste visible tant
 * qu'elle n'a pas ete traitee.
 */
@Component({
  selector: 'app-demo-request-list',
  imports: [FormsModule, DatePipe, Icon],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Demandes de démonstration</h1>
          <p class="page-header__subtitle">
            Prospects qui ont rempli le formulaire de la page commerciale
          </p>
        </div>
        <label class="filtre">
          <input type="checkbox" [(ngModel)]="onlyPending" (ngModelChange)="load()" />
          <span>N afficher que les demandes en attente</span>
        </label>
      </div>

      <div class="card">
        @if (loading()) {
          <div class="loader">
            <span class="loader__spinner"></span>
            Chargement...
          </div>
        } @else if (page(); as p) {
          @if (p.content.length === 0) {
            <div class="empty">
              <span class="empty__icon"><app-icon name="send" [size]="24" /></span>
              <div class="empty__title">Aucune demande pour le moment</div>
              <p>
                Les demandes laissées sur la page d’accueil apparaîtront ici, et
                vous recevrez une notification a chaque nouvelle.
              </p>
            </div>
          } @else {
            <ul class="liste">
              @for (d of p.content; track d.id) {
                <li class="demande" [class.demande--traitee]="d.handled">
                  <div class="demande__corps">
                    <div class="row">
                      <span class="demande__nom">{{ d.contactName }}</span>
                      @if (d.companyName) {
                        <span class="badge status-new">{{ d.companyName }}</span>
                      }
                      @if (d.establishmentCount) {
                        <span class="badge status-closed">
                          {{ d.establishmentCount }} établissements
                        </span>
                      }
                      @if (d.handled) {
                        <span class="badge status-resolved">Traitée</span>
                      }
                      <div class="spacer"></div>
                      <span class="text-xs text-faint nowrap">
                        {{ d.createdAt | date: 'dd/MM/yyyy HH:mm' }}
                      </span>
                    </div>

                    <div class="demande__contact">
                      <a [href]="'mailto:' + d.email">{{ d.email }}</a>
                      @if (d.phone) {
                        <span class="text-faint">&middot;</span>
                        <a [href]="'tel:' + d.phone">{{ d.phone }}</a>
                      }
                    </div>

                    @if (d.message) {
                      <p class="demande__message">{{ d.message }}</p>
                    }
                  </div>

                  @if (!d.handled) {
                    <button
                      type="button"
                      class="btn btn--secondary btn--sm"
                      (click)="markHandled(d)"
                    >
                      Marquer traitée
                    </button>
                  }
                </li>
              }
            </ul>
          }
        }
      </div>
    </div>
  `,
  styles: `
    .filtre {
      display: flex;
      align-items: center;
      gap: var(--sp-2);
      font-size: 0.85rem;
      color: var(--text-muted);
    }

    .filtre input {
      width: auto;
    }

    .liste {
      list-style: none;
      margin: 0;
      padding: 0;
    }

    .demande {
      display: flex;
      align-items: flex-start;
      gap: var(--sp-4);
      padding: var(--sp-4) var(--sp-5);
      border-bottom: 1px solid var(--ink-100);
    }

    .demande:last-child {
      border-bottom: none;
    }

    .demande--traitee {
      opacity: 0.62;
    }

    .demande__corps {
      flex: 1;
      min-width: 0;
    }

    .demande__nom {
      font-weight: 600;
      font-size: 0.95rem;
    }

    .demande__contact {
      margin-top: var(--sp-2);
      font-size: 0.85rem;
      display: flex;
      gap: var(--sp-2);
      flex-wrap: wrap;
    }

    .demande__message {
      margin-top: var(--sp-3);
      padding: var(--sp-3) var(--sp-4);
      background: var(--ink-50);
      border-radius: var(--radius-sm);
      font-size: 0.88rem;
      white-space: pre-wrap;
      line-height: 1.6;
    }
  `,
})
export class DemoRequestListPage {
  private readonly service = inject(DemoRequestService);
  private readonly toast = inject(ToastService);

  protected readonly page = signal<Page<DemoRequest> | null>(null);
  protected readonly loading = signal(true);

  protected onlyPending = false;

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.service.list(this.onlyPending).subscribe({
      next: (result) => {
        this.page.set(result);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected markHandled(demande: DemoRequest): void {
    this.service.markHandled(demande.id).subscribe({
      next: () => {
        this.toast.success(`Demande de ${demande.contactName} marquée comme traitée.`);
        this.load();
      },
      error: () => undefined,
    });
  }
}
