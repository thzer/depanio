import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { EstablishmentResponse, Page } from '../../core/models/api.models';
import { EnumOption, EstablishmentType } from '../../core/models/enums';
import { AuthService } from '../../core/services/auth.service';
import { EstablishmentService } from '../../core/services/establishment.service';
import { ReferenceService } from '../../core/services/reference.service';
import { ToastService } from '../../core/services/toast.service';
import { Icon } from '../../shared/icon';
import { Modal } from '../../shared/modal';

/**
 * Portefeuille de clients : consultation, creation et modification.
 *
 * Chaque ligne indique le nombre de tickets ouverts, ce qui fait
 * immediatement ressortir le client qui accumule les demandes.
 */
@Component({
  selector: 'app-establishment-list',
  imports: [FormsModule, ReactiveFormsModule, Icon, Modal],
  templateUrl: './establishment-list.html',
  styles: `
    .filters {
      display: grid;
      grid-template-columns: 2fr 1fr auto;
      gap: var(--sp-4);
      align-items: end;
    }

    .form-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: var(--sp-4);
    }

    .form-grid__full {
      grid-column: 1 / -1;
    }

    @media (max-width: 900px) {
      .filters,
      .form-grid {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class EstablishmentListPage {
  private readonly establishmentService = inject(EstablishmentService);
  private readonly referenceService = inject(ReferenceService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  protected readonly auth = inject(AuthService);

  protected readonly page = signal<Page<EstablishmentResponse> | null>(null);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly types = signal<EnumOption[]>([]);

  /** Etablissement en cours de modification ; null pour une creation. */
  protected readonly editing = signal<EstablishmentResponse | null>(null);
  protected readonly formOpen = signal(false);

  protected search = '';
  protected type: EstablishmentType | '' = '';

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    type: ['CAFE' as EstablishmentType, [Validators.required]],
    city: ['', [Validators.required]],
    address: [''],
    phone: [''],
    contactName: [''],
    contactEmail: ['', [Validators.email]],
    contractNumber: [''],
  });

  constructor() {
    this.referenceService
      .all()
      .subscribe((reference) => this.types.set(reference.establishmentTypes));
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.establishmentService
      .search(this.search || undefined, this.type || undefined, undefined, 0, 50)
      .subscribe({
        next: (result) => {
          this.page.set(result);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  protected resetFilters(): void {
    this.search = '';
    this.type = '';
    this.load();
  }

  // ------------------------------------------------- Creation et modification

  protected openCreate(): void {
    this.editing.set(null);
    this.form.reset({ type: 'CAFE' });
    this.formOpen.set(true);
  }

  protected openEdit(establishment: EstablishmentResponse): void {
    this.editing.set(establishment);
    this.form.reset({
      name: establishment.name,
      type: establishment.type,
      city: establishment.city,
      address: establishment.address ?? '',
      phone: establishment.phone ?? '',
      contactName: establishment.contactName ?? '',
      contactEmail: establishment.contactEmail ?? '',
      contractNumber: establishment.contractNumber ?? '',
    });
    this.formOpen.set(true);
  }

  protected closeForm(): void {
    this.formOpen.set(false);
  }

  protected submit(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const payload = {
      name: raw.name.trim(),
      type: raw.type,
      city: raw.city.trim(),
      address: raw.address || undefined,
      phone: raw.phone || undefined,
      contactName: raw.contactName || undefined,
      contactEmail: raw.contactEmail || undefined,
      contractNumber: raw.contractNumber || undefined,
    };

    this.saving.set(true);
    const existing = this.editing();

    const request = existing
      ? this.establishmentService.update(existing.id, payload)
      : this.establishmentService.create(payload);

    request.subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.formOpen.set(false);
        this.toast.success(
          existing ? `${saved.name} a ete mis a jour.` : `${saved.name} a ete enregistre.`,
        );
        this.load();
      },
      error: () => this.saving.set(false),
    });
  }

  /**
   * Active ou desactive un etablissement.
   * Le serveur refuse la desactivation s il reste des tickets ouverts ;
   * le message correspondant est alors affiche a l utilisateur.
   */
  protected toggleActive(establishment: EstablishmentResponse): void {
    this.establishmentService.setActive(establishment.id, !establishment.active).subscribe({
      next: (updated) => {
        this.toast.success(
          updated.active
            ? `${updated.name} est de nouveau actif.`
            : `${updated.name} a ete desactive.`,
        );
        this.load();
      },
      // Le message precis provient de l intercepteur d erreurs
      error: () => undefined,
    });
  }
}
