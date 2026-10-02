import { Component, inject, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DemoRequestService } from '../../core/services/demo-request.service';
import { Icon } from '../../shared/icon';
import { Modal } from '../../shared/modal';

/**
 * Formulaire de demande de demonstration.
 *
 * Il remplace une adresse {@code mailto:}, qui n'ouvrait rien chez les
 * visiteurs sans logiciel de messagerie configure — c'est-a-dire chez la
 * plupart. Un bouton qui ne fait rien coute un prospect a chaque clic.
 *
 * La demande part vers le serveur, qui l'enregistre et previent les
 * administrateurs. Elle survit donc a une panne de messagerie, et reste
 * visible tant qu'elle n'a pas ete traitee.
 */
@Component({
  selector: 'app-demo-form',
  imports: [ReactiveFormsModule, Icon, Modal],
  template: `
    <app-modal
      [title]="sent() ? 'Demande envoyee' : 'Demander une demonstration'"
      [subtitle]="
        sent()
          ? ''
          : 'Nous vous rappelons sous 24 heures pour convenir d un creneau.'
      "
      [width]="560"
      (closed)="closed.emit()"
    >
      @if (sent()) {
        <div class="done">
          <span class="done__icon"><app-icon name="check" [size]="26" /></span>
          <p class="done__title">Merci, votre demande est bien enregistree.</p>
          <p class="text-muted">
            Nous revenons vers vous sous 24 heures, a l adresse que vous avez
            indiquee.
          </p>
          <button type="button" class="btn btn--primary mt-5" (click)="closed.emit()">
            Fermer
          </button>
        </div>
      } @else {
        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="grid-2">
            <div class="field">
              <label class="field__label" for="d-name">Votre nom</label>
              <input
                id="d-name"
                type="text"
                formControlName="contactName"
                placeholder="Ayoub Borji"
                [class.invalid]="
                  form.controls.contactName.touched && form.controls.contactName.invalid
                "
              />
              @if (form.controls.contactName.touched && form.controls.contactName.invalid) {
                <span class="field__error">Votre nom est obligatoire.</span>
              }
            </div>

            <div class="field">
              <label class="field__label" for="d-company">Societe</label>
              <input
                id="d-company"
                type="text"
                formControlName="companyName"
                placeholder="Votre societe de maintenance"
              />
            </div>

            <div class="field">
              <label class="field__label" for="d-email">Adresse electronique</label>
              <input
                id="d-email"
                type="email"
                formControlName="email"
                placeholder="vous@votre-societe.tn"
                [class.invalid]="form.controls.email.touched && form.controls.email.invalid"
              />
              @if (form.controls.email.touched && form.controls.email.invalid) {
                <span class="field__error">Adresse electronique invalide.</span>
              }
            </div>

            <div class="field">
              <label class="field__label" for="d-phone">Telephone</label>
              <input
                id="d-phone"
                type="tel"
                formControlName="phone"
                placeholder="98 000 000"
              />
            </div>

            <div class="field grid-2__full">
              <label class="field__label" for="d-count">
                Combien d etablissements suivez-vous ?
              </label>
              <select id="d-count" formControlName="establishmentCount">
                <option value="">Preferer ne pas repondre</option>
                <option value="1-5">1 a 5</option>
                <option value="6-15">6 a 15</option>
                <option value="16-40">16 a 40</option>
                <option value="40+">Plus de 40</option>
              </select>
              <span class="field__hint">
                Cela nous permet de preparer une demonstration a votre echelle.
              </span>
            </div>

            <div class="field grid-2__full">
              <label class="field__label" for="d-message">Votre message</label>
              <textarea
                id="d-message"
                rows="3"
                formControlName="message"
                placeholder="Facultatif : ce que vous cherchez a resoudre."
              ></textarea>
            </div>
          </div>

          <!--
            Champ piege : invisible pour un visiteur, mais rempli par les
            robots qui soumettent tous les champs d un formulaire.
          -->
          <input
            class="trap"
            type="text"
            formControlName="website"
            tabindex="-1"
            autocomplete="off"
            aria-hidden="true"
          />

          @if (errorMessage()) {
            <div class="alert mt-4">
              <app-icon name="alert" [size]="18" />
              <span>{{ errorMessage() }}</span>
            </div>
          }

          <div class="row mt-5">
            <button type="submit" class="btn btn--primary btn--lg" [disabled]="sending()">
              {{ sending() ? 'Envoi...' : 'Envoyer ma demande' }}
            </button>
            <button type="button" class="btn btn--secondary" (click)="closed.emit()">
              Annuler
            </button>
          </div>
        </form>
      }
    </app-modal>
  `,
  styles: `
    .grid-2 {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: var(--sp-4);
    }

    .grid-2__full {
      grid-column: 1 / -1;
    }

    /* Hors du flux et hors du champ de vision, mais present dans le DOM :
       c est ce qui le rend efficace contre les envois automatises. */
    .trap {
      position: absolute;
      left: -9999px;
      width: 1px;
      height: 1px;
      opacity: 0;
    }

    .alert {
      display: flex;
      align-items: flex-start;
      gap: var(--sp-3);
      padding: var(--sp-3) var(--sp-4);
      border-radius: var(--radius-sm);
      background: var(--danger-bg);
      border: 1px solid var(--danger-border);
      color: var(--danger);
      font-size: 0.85rem;
      font-weight: 500;
    }

    .done {
      text-align: center;
      padding: var(--sp-4) 0;
    }

    .done__icon {
      display: grid;
      place-items: center;
      width: 56px;
      height: 56px;
      margin: 0 auto var(--sp-4);
      border-radius: 50%;
      background: var(--success-bg);
      color: var(--success);
    }

    .done__title {
      font-family: var(--font-display);
      font-size: 1.05rem;
      font-weight: 700;
      margin-bottom: var(--sp-2);
    }

    @media (max-width: 560px) {
      .grid-2 {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class DemoForm {
  readonly closed = output<void>();

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(DemoRequestService);

  protected readonly sending = signal(false);
  protected readonly sent = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    contactName: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    companyName: [''],
    establishmentCount: [''],
    message: [''],
    website: [''],
  });

  protected submit(): void {
    if (this.form.invalid || this.sending()) {
      this.form.markAllAsTouched();
      return;
    }

    this.sending.set(true);
    this.errorMessage.set(null);

    this.service.submit(this.form.getRawValue()).subscribe({
      next: () => {
        this.sending.set(false);
        this.sent.set(true);
      },
      error: (error) => {
        this.sending.set(false);
        this.errorMessage.set(
          error?.error?.message ??
            'L envoi a echoue. Reessayez, ou ecrivez-nous directement.',
        );
      },
    });
  }
}
