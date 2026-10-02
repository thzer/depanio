import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { Icon } from '../../shared/icon';

/**
 * Changement de son propre mot de passe.
 *
 * Indispensable des lors que les comptes sont crees par un
 * administrateur : sans cet ecran, celui-ci connaitrait indefiniment le
 * mot de passe de chacun de ses clients et de ses techniciens.
 *
 * L'ancien mot de passe est exige par le serveur : un poste laisse
 * ouvert ou un jeton derobe ne suffit alors pas a s'approprier le compte.
 */
@Component({
  selector: 'app-change-password',
  imports: [ReactiveFormsModule, Icon],
  templateUrl: './change-password.html',
  styleUrl: './change-password.scss',
})
export class ChangePasswordPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  protected readonly user = this.auth.currentUser;
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly visible = signal(false);

  protected readonly form = this.fb.nonNullable.group(
    {
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(10)]],
      confirmation: ['', [Validators.required]],
    },
    { validators: [passwordsMatch] },
  );

  /**
   * Indicateurs de solidite affiches pendant la saisie.
   * Ils guident sans bloquer : seule la longueur minimale est exigee,
   * le reste est un conseil.
   */
  protected readonly checks = signal<{ label: string; ok: boolean }[]>([]);

  constructor() {
    this.form.controls.newPassword.valueChanges.subscribe((value) => {
      this.checks.set([
        { label: 'Au moins 10 caracteres', ok: value.length >= 10 },
        { label: 'Une majuscule', ok: /[A-Z]/.test(value) },
        { label: 'Un chiffre', ok: /[0-9]/.test(value) },
        { label: 'Un caractere special', ok: /[^A-Za-z0-9]/.test(value) },
      ]);
    });
  }

  protected toggleVisible(): void {
    this.visible.update((v) => !v);
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    this.submitting.set(true);
    this.errorMessage.set(null);

    this.auth
      .changePassword({
        currentPassword: raw.currentPassword,
        newPassword: raw.newPassword,
      })
      .subscribe({
        next: () => {
          this.toast.success('Votre mot de passe a ete modifie.');
          void this.router.navigate(['/tableau-de-bord']);
        },
        error: (error) => {
          this.submitting.set(false);
          this.errorMessage.set(
            error?.error?.message ?? 'La modification a echoue. Reessayez.',
          );
        },
      });
  }

  protected cancel(): void {
    void this.router.navigate(['/tableau-de-bord']);
  }
}

/**
 * Verifie que la confirmation correspond au nouveau mot de passe.
 * Place sur le groupe et non sur un champ : la regle porte sur deux
 * valeurs a la fois.
 */
function passwordsMatch(group: AbstractControl): { mismatch: true } | null {
  const nouveau = group.get('newPassword')?.value;
  const confirmation = group.get('confirmation')?.value;
  return nouveau && confirmation && nouveau !== confirmation ? { mismatch: true } : null;
}
