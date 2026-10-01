import { Component, inject } from '@angular/core';
import { ToastService } from '../core/services/toast.service';

/**
 * Pile de messages temporaires affichee en haut a droite de l'ecran.
 *
 * Le composant est place une seule fois a la racine de l'application :
 * n'importe quel service peut ainsi afficher un message sans avoir besoin
 * d'une reference a un composant.
 */
@Component({
  selector: 'app-toast-container',
  template: `
    <div class="toasts" role="status" aria-live="polite">
      @for (toast of toastService.toasts(); track toast.id) {
        <div class="toast toast--{{ toast.kind }}">
          <span class="toast__message">{{ toast.message }}</span>
          <button
            type="button"
            class="toast__close"
            aria-label="Fermer"
            (click)="toastService.dismiss(toast.id)"
          >
            &times;
          </button>
        </div>
      }
    </div>
  `,
  styles: `
    .toasts {
      position: fixed;
      top: 16px;
      right: 16px;
      z-index: 1000;
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-width: 380px;
    }

    .toast {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 12px 14px;
      border-radius: 10px;
      border: 1px solid;
      box-shadow: var(--shadow-lg);
      background: var(--surface);
      animation: slide-in 0.2s ease-out;
    }

    .toast__message {
      flex: 1;
      font-size: 0.875rem;
      line-height: 1.45;
    }

    .toast__close {
      background: none;
      border: none;
      font-size: 1.15rem;
      line-height: 1;
      cursor: pointer;
      color: inherit;
      opacity: 0.6;
      padding: 0;
    }

    .toast__close:hover {
      opacity: 1;
    }

    .toast--success {
      border-color: var(--success);
      background: var(--success-light);
      color: var(--success);
    }

    .toast--error {
      border-color: var(--danger);
      background: var(--danger-light);
      color: var(--danger);
    }

    .toast--info {
      border-color: var(--info);
      background: var(--info-light);
      color: var(--info);
    }

    @keyframes slide-in {
      from {
        opacity: 0;
        transform: translateX(16px);
      }
      to {
        opacity: 1;
        transform: translateX(0);
      }
    }
  `,
})
export class ToastContainer {
  protected readonly toastService = inject(ToastService);
}
