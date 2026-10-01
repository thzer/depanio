import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NotificationResponse, Page } from '../../core/models/api.models';
import { NotificationService } from '../../core/services/notification.service';

/**
 * Journal des notifications de l utilisateur connecte.
 *
 * Les notifications restent consultables apres coup : une alerte de
 * depassement de delai arrivee pendant une intervention ne doit pas etre
 * perdue parce que le navigateur etait ferme.
 */
@Component({
  selector: 'app-notification-list',
  imports: [RouterLink, DatePipe],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Notifications</h1>
          <p class="page-header__subtitle">
            {{ notifications.unreadCount() }} notification(s) non lue(s)
          </p>
        </div>
        <button
          type="button"
          class="btn btn--secondary"
          [disabled]="notifications.unreadCount() === 0"
          (click)="markAll()"
        >
          Tout marquer comme lu
        </button>
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
              <div class="empty__title">Aucune notification</div>
              <p>Les alertes sur vos tickets apparaitront ici.</p>
            </div>
          } @else {
            <ul class="list">
              @for (item of p.content; track item.id) {
                <li class="item" [class.item--unread]="!item.read">
                  <div class="item__main">
                    <div class="row">
                      <span class="badge status-new">{{ item.typeLabel }}</span>
                      @if (!item.read) {
                        <span class="dot" title="Non lue"></span>
                      }
                      <div class="spacer"></div>
                      <span class="text-xs text-faint">
                        {{ item.createdAt | date: 'dd/MM/yyyy HH:mm' }}
                      </span>
                    </div>
                    <p class="item__message">{{ item.message }}</p>
                    @if (item.ticketId) {
                      <a class="text-sm" [routerLink]="['/tickets', item.ticketId]">
                        Ouvrir le ticket {{ item.ticketReference }}
                      </a>
                    }
                  </div>
                  @if (!item.read) {
                    <button
                      type="button"
                      class="btn btn--ghost btn--sm"
                      (click)="markOne(item)"
                    >
                      Marquer comme lu
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
    .list {
      list-style: none;
      margin: 0;
      padding: 0;
    }

    .item {
      display: flex;
      align-items: flex-start;
      gap: var(--sp-4);
      padding: var(--sp-4) var(--sp-5);
      border-bottom: 1px solid var(--gray-100);
    }

    .item:last-child {
      border-bottom: none;
    }

    .item--unread {
      background: var(--primary-light);
    }

    .item__main {
      flex: 1;
      min-width: 0;
    }

    .item__message {
      margin-top: var(--sp-2);
      font-size: 0.9rem;
    }

    .dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: var(--primary);
    }
  `,
})
export class NotificationListPage {
  protected readonly notifications = inject(NotificationService);

  protected readonly page = signal<Page<NotificationResponse> | null>(null);
  protected readonly loading = signal(true);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.notifications.list(0, 50).subscribe({
      next: (result) => {
        this.page.set(result);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected markOne(item: NotificationResponse): void {
    this.notifications.markAsRead(item.id).subscribe(() => this.load());
  }

  protected markAll(): void {
    this.notifications.markAllAsRead().subscribe(() => this.load());
  }
}
