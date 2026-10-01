import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EstablishmentResponse, Page } from '../../core/models/api.models';
import { EnumOption, EstablishmentType } from '../../core/models/enums';
import { AuthService } from '../../core/services/auth.service';
import { EstablishmentService } from '../../core/services/establishment.service';
import { ReferenceService } from '../../core/services/reference.service';
import { ToastService } from '../../core/services/toast.service';

/**
 * Portefeuille de clients.
 *
 * Chaque ligne indique le nombre de tickets ouverts : un client qui
 * accumule les demandes ouvertes se repere ainsi immediatement.
 */
@Component({
  selector: 'app-establishment-list',
  imports: [FormsModule],
  templateUrl: './establishment-list.html',
  styles: `
    .filters {
      display: grid;
      grid-template-columns: 2fr 1fr auto;
      gap: var(--sp-4);
      align-items: end;
    }

    @media (max-width: 900px) {
      .filters {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class EstablishmentListPage {
  private readonly establishmentService = inject(EstablishmentService);
  private readonly referenceService = inject(ReferenceService);
  private readonly toast = inject(ToastService);
  protected readonly auth = inject(AuthService);

  protected readonly page = signal<Page<EstablishmentResponse> | null>(null);
  protected readonly loading = signal(true);
  protected readonly types = signal<EnumOption[]>([]);

  protected search = '';
  protected type: EstablishmentType | '' = '';

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
