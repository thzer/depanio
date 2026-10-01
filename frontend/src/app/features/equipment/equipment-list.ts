import { DatePipe } from '@angular/common';
import { Component, OnDestroy, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EquipmentResponse, EstablishmentResponse, Page } from '../../core/models/api.models';
import { EnumOption, EquipmentType } from '../../core/models/enums';
import { AuthService } from '../../core/services/auth.service';
import { EquipmentService } from '../../core/services/equipment.service';
import { EstablishmentService } from '../../core/services/establishment.service';
import { ReferenceService } from '../../core/services/reference.service';
import { ToastService } from '../../core/services/toast.service';

/**
 * Parc de materiel et impression des etiquettes QR.
 *
 * L image de l etiquette est recuperee par un appel authentifie puis
 * convertie en URL locale : une balise img classique ne transmettrait pas
 * le jeton et le serveur refuserait la requete.
 */
@Component({
  selector: 'app-equipment-list',
  imports: [FormsModule, DatePipe],
  templateUrl: './equipment-list.html',
  styleUrl: './equipment-list.scss',
})
export class EquipmentListPage implements OnDestroy {
  private readonly equipmentService = inject(EquipmentService);
  private readonly establishmentService = inject(EstablishmentService);
  private readonly referenceService = inject(ReferenceService);
  private readonly toast = inject(ToastService);
  protected readonly auth = inject(AuthService);

  protected readonly page = signal<Page<EquipmentResponse> | null>(null);
  protected readonly loading = signal(true);
  protected readonly types = signal<EnumOption[]>([]);
  protected readonly establishments = signal<EstablishmentResponse[]>([]);

  /** Materiel dont l etiquette est affichee en grand pour impression. */
  protected readonly qrEquipment = signal<EquipmentResponse | null>(null);
  protected readonly qrImageUrl = signal<string | null>(null);

  protected search = '';
  protected type: EquipmentType | '' = '';
  protected establishmentId: number | null = null;

  constructor() {
    this.referenceService.all().subscribe((reference) => this.types.set(reference.equipmentTypes));

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
      .search(this.establishmentId ?? undefined, this.type || undefined, this.search || undefined)
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

  /**
   * Libere l URL temporaire de l image.
   * Sans cette liberation, chaque etiquette consultee resterait en memoire
   * jusqu au rechargement de la page.
   */
  private releaseQrImage(): void {
    const url = this.qrImageUrl();
    if (url) {
      URL.revokeObjectURL(url);
      this.qrImageUrl.set(null);
    }
  }
}
