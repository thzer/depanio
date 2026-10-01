import { DatePipe } from '@angular/common';
import { Component, OnDestroy, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { EquipmentResponse, EstablishmentResponse, Page } from '../../core/models/api.models';
import { EnumOption, EquipmentType } from '../../core/models/enums';
import { AuthService } from '../../core/services/auth.service';
import { EquipmentService } from '../../core/services/equipment.service';
import { EstablishmentService } from '../../core/services/establishment.service';
import { ReferenceService } from '../../core/services/reference.service';
import { ToastService } from '../../core/services/toast.service';
import { Icon } from '../../shared/icon';
import { Modal } from '../../shared/modal';

/**
 * Parc de materiel, etiquettes QR et saisie des equipements.
 *
 * L image de l etiquette est recuperee par un appel authentifie puis
 * convertie en adresse locale : une balise img ordinaire ne transmettrait
 * pas le jeton, et le serveur refuserait la requete.
 */
@Component({
  selector: 'app-equipment-list',
  imports: [FormsModule, ReactiveFormsModule, DatePipe, Icon, Modal],
  templateUrl: './equipment-list.html',
  styleUrl: './equipment-list.scss',
})
export class EquipmentListPage implements OnDestroy {
  private readonly equipmentService = inject(EquipmentService);
  private readonly establishmentService = inject(EstablishmentService);
  private readonly referenceService = inject(ReferenceService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  protected readonly auth = inject(AuthService);

  protected readonly page = signal<Page<EquipmentResponse> | null>(null);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly types = signal<EnumOption[]>([]);
  protected readonly establishments = signal<EstablishmentResponse[]>([]);

  /** Materiel dont l etiquette est affichee en grand pour impression. */
  protected readonly qrEquipment = signal<EquipmentResponse | null>(null);
  protected readonly qrImageUrl = signal<string | null>(null);

  /** Materiel en cours de modification ; null pour une creation. */
  protected readonly editing = signal<EquipmentResponse | null>(null);
  protected readonly formOpen = signal(false);

  protected search = '';
  protected type: EquipmentType | '' = '';
  protected establishmentId: number | null = null;

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    type: ['POS_TERMINAL' as EquipmentType, [Validators.required]],
    establishmentId: [null as number | null, [Validators.required]],
    brand: [''],
    model: [''],
    serialNumber: [''],
    location: [''],
    installedAt: [''],
    warrantyEnd: [''],
  });

  constructor() {
    this.referenceService
      .all()
      .subscribe((reference) => this.types.set(reference.equipmentTypes));

    if (!this.auth.isClient()) {
      this.establishmentService.active().subscribe((list) => this.establishments.set(list));
    }

    this.load();
  }

  ngOnDestroy(): void {
    this.releaseQrImage();
  }

  protected load(): void {
    this.loading.set(true);
    this.equipmentService
      .search(
        this.establishmentId ?? undefined,
        this.type || undefined,
        this.search || undefined,
      )
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
    this.establishmentId = null;
    this.load();
  }

  // ------------------------------------------------- Creation et modification

  protected openCreate(): void {
    this.editing.set(null);
    this.form.reset({
      type: 'POS_TERMINAL',
      // Si un seul client existe, il est pre-selectionne : un geste de moins
      establishmentId:
        this.establishments().length === 1 ? this.establishments()[0].id : null,
    });
    this.formOpen.set(true);
  }

  protected openEdit(equipment: EquipmentResponse): void {
    this.editing.set(equipment);
    this.form.reset({
      name: equipment.name,
      type: equipment.type,
      establishmentId: equipment.establishmentId,
      brand: equipment.brand ?? '',
      model: equipment.model ?? '',
      serialNumber: equipment.serialNumber ?? '',
      location: equipment.location ?? '',
      installedAt: equipment.installedAt ?? '',
      warrantyEnd: equipment.warrantyEnd ?? '',
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
      establishmentId: raw.establishmentId!,
      brand: raw.brand || undefined,
      model: raw.model || undefined,
      serialNumber: raw.serialNumber || undefined,
      location: raw.location || undefined,
      installedAt: raw.installedAt || undefined,
      warrantyEnd: raw.warrantyEnd || undefined,
    };

    this.saving.set(true);
    const existing = this.editing();

    const request = existing
      ? this.equipmentService.update(existing.id, payload)
      : this.equipmentService.create(payload);

    request.subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.formOpen.set(false);
        this.toast.success(
          existing
            ? `${saved.name} a ete mis a jour.`
            : `${saved.name} a ete enregistre. Son etiquette QR est prete.`,
        );
        this.load();
      },
      error: () => this.saving.set(false),
    });
  }

  // ------------------------------------------------------------ Etiquettes QR

  protected showQrCode(equipment: EquipmentResponse): void {
    this.releaseQrImage();
    this.qrEquipment.set(equipment);

    this.equipmentService.qrCodeBlob(equipment.id, 420).subscribe({
      next: (blob) => this.qrImageUrl.set(URL.createObjectURL(blob)),
      error: () => this.toast.error('Impossible de generer l etiquette.'),
    });
  }

  protected closeQrCode(): void {
    this.releaseQrImage();
    this.qrEquipment.set(null);
  }

  protected printQrCode(): void {
    window.print();
  }

  protected setActive(equipment: EquipmentResponse, active: boolean): void {
    this.equipmentService.setActive(equipment.id, active).subscribe({
      next: () => {
        this.toast.success(
          active ? `${equipment.name} est remis en service.` : `${equipment.name} est hors service.`,
        );
        this.load();
      },
      error: () => undefined,
    });
  }

  /**
   * Libere l adresse temporaire de l image.
   * Sans cela, chaque etiquette consultee resterait en memoire jusqu au
   * rechargement de la page.
   */
  private releaseQrImage(): void {
    const url = this.qrImageUrl();
    if (url) {
      URL.revokeObjectURL(url);
      this.qrImageUrl.set(null);
    }
  }
}
