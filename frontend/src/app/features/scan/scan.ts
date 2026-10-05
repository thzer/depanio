import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { EquipmentResponse } from '../../core/models/api.models';
import { EnumOption, Priority, TicketCategory } from '../../core/models/enums';
import { EquipmentService } from '../../core/services/equipment.service';
import { ReferenceService } from '../../core/services/reference.service';
import { TicketService } from '../../core/services/ticket.service';
import { ToastService } from '../../core/services/toast.service';

/**
 * Ecran ouvert par la lecture d une etiquette QR collee sur une machine.
 *
 * Le jeton contenu dans l adresse identifie le materiel : le gerant n a
 * donc ni etablissement ni equipement a selectionner, ce qui reduit la
 * declaration a trois champs. C est l interet principal des etiquettes :
 * une panne signalee en quelques secondes, depuis le telephone, sans
 * chercher la machine dans une liste.
 */
@Component({
  selector: 'app-scan',
  imports: [ReactiveFormsModule],
  template: `
    <div class="page" style="max-width: 640px">
      @if (loading()) {
        <div class="loader">
          <span class="loader__spinner"></span>
          Identification du matériel…
        </div>
      } @else if (equipment(); as e) {
        <div class="page-header">
          <div>
            <h1>Déclarer une panne</h1>
            <p class="page-header__subtitle">
              Matériel identifié par son étiquette QR
            </p>
          </div>
        </div>

        <div class="card mb-4">
          <div class="card__body">
            <div class="identified">
              <div>
                <div class="text-xs text-faint">Matériel</div>
                <div class="identified__name">{{ e.name }}</div>
                <div class="text-sm text-muted">
                  {{ e.brand }} {{ e.model }}
                  @if (e.location) {
                    &middot; {{ e.location }}
                  }
                </div>
                <div class="text-sm text-muted">{{ e.establishmentName }}</div>
              </div>
              <span class="badge" [class.status-resolved]="e.underWarranty"
                [class.status-closed]="!e.underWarranty">
                {{ e.underWarranty ? 'Sous garantie' : 'Hors garantie' }}
              </span>
            </div>
          </div>
        </div>

        <div class="card">
          <form class="card__body stack" [formGroup]="form" (ngSubmit)="submit()">
            <div class="field">
              <label class="field__label" for="title">Que se passe-t-il ?</label>
              <input
                id="title"
                type="text"
                formControlName="title"
                placeholder="Par exemple : la machine ne demarre plus"
                [class.invalid]="form.controls.title.touched && form.controls.title.invalid"
              />
            </div>

            <div class="field">
              <label class="field__label" for="description">Détails</label>
              <textarea
                id="description"
                formControlName="description"
                rows="5"
                placeholder="Depuis quand ? Un message d’erreur s’affiche-t-il ?"
                [class.invalid]="
                  form.controls.description.touched && form.controls.description.invalid
                "
              ></textarea>
            </div>

            <div class="grid grid--2">
              <div class="field">
                <label class="field__label" for="category">Nature du problème</label>
                <select id="category" formControlName="category">
                  <option value="">Choisir</option>
                  @for (option of categories(); track option.value) {
                    <option [value]="option.value">{{ option.label }}</option>
                  }
                </select>
              </div>

              <div class="field">
                <label class="field__label" for="priority">Urgence</label>
                <select id="priority" formControlName="priority">
                  @for (option of priorities(); track option.value) {
                    <option [value]="option.value">{{ option.label }}</option>
                  }
                </select>
              </div>
            </div>

            <button
              type="submit"
              class="btn btn--primary"
              [disabled]="submitting() || form.invalid"
            >
              {{ submitting() ? 'Envoi en cours...' : 'Envoyer la declaration' }}
            </button>
          </form>
        </div>
      } @else {
        <div class="card">
          <div class="empty">
            <div class="empty__title">Étiquette non reconnue</div>
            <p>
              Ce code ne correspond à aucun matériel de votre établissement.
              Vérifiez l’étiquette ou contactez votre administrateur.
            </p>
            <button type="button" class="btn btn--primary mt-4" (click)="goToTickets()">
              Retour aux tickets
            </button>
          </div>
        </div>
      }
    </div>
  `,
  styles: `
    .identified {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: var(--sp-4);
    }

    .identified__name {
      font-size: 1.1rem;
      font-weight: 700;
      color: var(--primary);
    }
  `,
})
export class ScanPage {
  /** Jeton lu dans l etiquette QR, transmis par l adresse. */
  readonly qrToken = input.required<string>();

  private readonly fb = inject(FormBuilder);
  private readonly equipmentService = inject(EquipmentService);
  private readonly ticketService = inject(TicketService);
  private readonly referenceService = inject(ReferenceService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  protected readonly equipment = signal<EquipmentResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);
  protected readonly categories = signal<EnumOption[]>([]);
  protected readonly priorities = signal<EnumOption[]>([]);

  protected readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['', [Validators.required, Validators.minLength(10)]],
    category: ['HARDWARE_FAILURE' as TicketCategory | '', [Validators.required]],
    priority: ['HIGH' as Priority, [Validators.required]],
  });

  constructor() {
    this.referenceService.all().subscribe((reference) => {
      this.categories.set(reference.categories);
      this.priorities.set(reference.priorities);
    });

    queueMicrotask(() => this.identify());
  }

  private identify(): void {
    this.equipmentService.byQrToken(this.qrToken()).subscribe({
      next: (equipment) => {
        this.equipment.set(equipment);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    this.submitting.set(true);

    this.ticketService
      .createFromQr(this.qrToken(), {
        title: raw.title,
        description: raw.description,
        category: raw.category as TicketCategory,
        priority: raw.priority,
      })
      .subscribe({
        next: (ticket) => {
          this.toast.success(`Ticket ${ticket.reference} enregistre.`);
          void this.router.navigate(['/tickets', ticket.id]);
        },
        error: () => this.submitting.set(false),
      });
  }

  protected goToTickets(): void {
    void this.router.navigate(['/tickets']);
  }
}
