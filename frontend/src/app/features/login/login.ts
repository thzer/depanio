import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/services/auth.service';

/**
 * Ecran de connexion.
 *
 * Les comptes de demonstration sont proposes en un clic : ils evitent de
 * ressaisir les identifiants a chaque essai pendant le developpement et
 * pendant la soutenance. Ce bloc n'est affiche qu'en developpement.
 */
@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly appName = environment.appName;
  protected readonly showDemoAccounts = !environment.production;

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  /** Comptes du jeu de demonstration, pour un essai immediat. */
  protected readonly demoAccounts = [
    { label: 'Administrateur', email: 'admin@helpdesk.tn' },
    { label: 'Technicien', email: 'ahmed.tech@helpdesk.tn' },
    { label: 'Gerant de cafe', email: 'hichem@elmedina.tn' },
  ];

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
        // L'adresse demandee avant la connexion est restauree si elle existe
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
