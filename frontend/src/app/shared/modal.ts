import { Component, HostListener, input, output } from '@angular/core';
import { Icon } from './icon';

/**
 * Fenetre de saisie superposee a l ecran courant.
 *
 * Utilisee pour la creation et la modification : l utilisateur garde sous
 * les yeux la liste qu il est en train d enrichir, et revient exactement
 * ou il en etait apres validation.
 *
 * La fermeture est possible par la croix, par la touche Echap et par un
 * clic sur le fond, comme on s y attend d une telle fenetre.
 */
@Component({
  selector: 'app-modal',
  imports: [Icon],
  template: `
    <div class="backdrop" (click)="onBackdropClick($event)">
      <div
        class="modal"
        [style.max-width.px]="width()"
        role="dialog"
        aria-modal="true"
        (click)="$event.stopPropagation()"
      >
        <header class="modal__head">
          <div>
            <h2 class="modal__title">{{ title() }}</h2>
            @if (subtitle()) {
              <p class="modal__subtitle">{{ subtitle() }}</p>
            }
          </div>
          <button type="button" class="modal__close" aria-label="Fermer" (click)="closed.emit()">
            <app-icon name="close" [size]="18" />
          </button>
        </header>

        <div class="modal__body">
          <ng-content />
        </div>
      </div>
    </div>
  `,
  styles: `
    .backdrop {
      position: fixed;
      inset: 0;
      z-index: 100;
      background: rgba(8, 13, 24, 0.5);
      backdrop-filter: blur(2px);
      display: flex;
      align-items: flex-start;
      justify-content: center;
      padding: var(--sp-6) var(--sp-4);
      overflow-y: auto;
      animation: fade 0.14s var(--ease);
    }

    .modal {
      width: 100%;
      background: var(--surface);
      border-radius: var(--radius-lg);
      box-shadow: var(--shadow-xl);
      margin: auto;
      animation: rise 0.18s var(--ease);
    }

    .modal__head {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: var(--sp-4);
      padding: var(--sp-5);
      border-bottom: 1px solid var(--border);
    }

    .modal__title {
      font-size: 1.1rem;
    }

    .modal__subtitle {
      margin-top: var(--sp-1);
      font-size: 0.85rem;
      color: var(--text-muted);
    }

    .modal__close {
      display: grid;
      place-items: center;
      width: 32px;
      height: 32px;
      flex-shrink: 0;
      border: none;
      background: none;
      border-radius: var(--radius-sm);
      color: var(--ink-500);
      cursor: pointer;
    }

    .modal__close:hover {
      background: var(--ink-100);
      color: var(--ink-800);
    }

    .modal__body {
      padding: var(--sp-5);
    }

    @keyframes fade {
      from {
        opacity: 0;
      }
    }

    @keyframes rise {
      from {
        opacity: 0;
        transform: translateY(10px);
      }
    }
  `,
})
export class Modal {
  readonly title = input.required<string>();
  readonly subtitle = input<string>();
  readonly width = input(620);

  readonly closed = output<void>();

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    this.closed.emit();
  }

  protected onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('backdrop')) {
      this.closed.emit();
    }
  }
}
