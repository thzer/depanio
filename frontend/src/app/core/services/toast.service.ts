import { Injectable, signal } from '@angular/core';

export type ToastKind = 'success' | 'error' | 'info';

export interface Toast {
  id: number;
  kind: ToastKind;
  message: string;
}

/**
 * Messages temporaires affiches en haut de l'ecran.
 *
 * Un seul service centralise ces messages : les composants et
 * l'intercepteur d'erreurs y deposent un texte, et le composant d'affichage
 * les presente. Chaque message disparait de lui-meme apres quelques
 * secondes.
 */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private static readonly DURATION_MS = 4500;

  private nextId = 1;
  private readonly items = signal<Toast[]>([]);

  readonly toasts = this.items.asReadonly();

  success(message: string): void {
    this.push('success', message);
  }

  error(message: string): void {
    this.push('error', message);
  }

  info(message: string): void {
    this.push('info', message);
  }

  dismiss(id: number): void {
    this.items.update((list) => list.filter((toast) => toast.id !== id));
  }

  private push(kind: ToastKind, message: string): void {
    const id = this.nextId++;
    this.items.update((list) => [...list, { id, kind, message }]);
    setTimeout(() => this.dismiss(id), ToastService.DURATION_MS);
  }
}
