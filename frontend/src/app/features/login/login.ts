import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/services/auth.service';
import { Icon } from '../../shared/icon';
import { Logo } from '../../shared/logo';

/**
 * Ecran de connexion.
 *
 * Les comptes de demonstration sont proposes en un clic : ils evitent de
 * ressaisir les identifiants pendant une presentation commerciale ou une
 * soutenance. Ce bloc disparait automatiquement en production.
 */
@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, Icon, Logo],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly showDemoAccounts = !environment.production;
  protected readonly year = new Date().getFullYear();

  protected readonly submitting = signal(false);
  protected readonly passwordVisible = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  /** Comptes du jeu de demonstration, pour un essai immediat. */
  protected readonly demoAccounts = [
    { label: 'Administrateur', email: 'admin@helpdesk.tn', icon: 'shield' },
    { label: 'Technicien', email: 'ahmed.tech@helpdesk.tn', icon: 'tool' },
    { label: 'Gerant de cafe', email: 'hichem@elmedina.tn', icon: 'store' },
  ];

  protected togglePassword(): void {
    this.passwordVisible.update((visible) => !visible);
  }

  protected fillDemoAccount(email: string): void {
    this.form.patchValue({ email, password: 'Passer@123' });
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);

    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        // L adresse demandee avant la connexion est restauree si elle existe
        const redirect = new URLSearchParams(window.location.search).get('redirect');
        void this.router.navigateByUrl(redirect ?? '/tableau-de-bord');
      },
      error: (error) => {
        this.submitting.set(false);
        this.errorMessage.set(
          error?.error?.message ?? 'Connexion impossible. Verifiez vos identifiants.',
        );
      },
    });
  }
}
