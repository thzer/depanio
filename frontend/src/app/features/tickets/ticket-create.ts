import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { EquipmentResponse, EstablishmentResponse } from '../../core/models/api.models';
import { EnumOption, Priority, TicketCategory } from '../../core/models/enums';
import { AuthService } from '../../core/services/auth.service';
import { EquipmentService } from '../../core/services/equipment.service';
import { EstablishmentService } from '../../core/services/establishment.service';
import { ReferenceService } from '../../core/services/reference.service';
import { TicketService } from '../../core/services/ticket.service';
import { ToastService } from '../../core/services/toast.service';

/**
 * Formulaire de declaration d un probleme.
 *
 * Le comportement dépend du role :
 *
 * - un **gerant** declare pour son propre etablissement, qui n est donc
 *   pas demande ; la liste de materiel est celle de son etablissement ;
 * - un **administrateur ou technicien** saisit souvent un appel
 *   telephonique : il choisit l etablissement, et la liste de materiel se
 *   recharge en consequence.
 */
@Component({
  selector: 'app-ticket-create',
  imports: [ReactiveFormsModule],
  templateUrl: './ticket-create.html',
})
export class TicketCreatePage {
  private readonly fb = inject(FormBuilder);
  private readonly ticketService = inject(TicketService);
  private readonly equipmentService = inject(EquipmentService);
  private readonly establishmentService = inject(EstablishmentService);
  private readonly referenceService = inject(ReferenceService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  protected readonly categories = signal<EnumOption[]>([]);
  protected readonly priorities = signal<EnumOption[]>([]);
  protected readonly establishments = signal<EstablishmentResponse[]>([]);
  protected readonly equipments = signal<EquipmentResponse[]>([]);
  protected readonly submitting = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['', [Validators.required, Validators.minLength(10)]],
    category: ['' as TicketCategory | '', [Validators.required]],
    priority: ['MEDIUM' as Priority, [Validators.required]],
    equipmentId: [null as number | null],
    establishmentId: [null as number | null],
  });

  constructor() {
    this.referenceService.all().subscribe((reference) => {
      this.categories.set(reference.categories);
      this.priorities.set(reference.priorities);
    });

    if (this.auth.isClient()) {
      // Le gerant ne choisit pas son etablissement : il est deduit du compte
      const establishmentId = this.auth.currentUser()?.establishmentId;
      if (establishmentId) {
        this.loadEquipments(establishmentId);
      }
    } else {
      this.establishmentService.active().subscribe((list) => this.establishments.set(list));
    }
  }

  /** Recharge la liste de materiel quand l etablissement change. */
  protected onEstablishmentChange(value: string): void {
    const establishmentId = value ? Number(value) : null;
    this.form.patchValue({ establishmentId, equipmentId: null });
    this.equipments.set([]);
    if (establishmentId) {
      this.loadEquipments(establishmentId);
    }
  }

  private loadEquipments(establishmentId: number): void {
    this.equipmentService
      .byEstablishment(establishmentId)
      .subscribe((list) => this.equipments.set(list));
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    this.submitting.set(true);

    this.ticketService
      .create({
        title: raw.title,
        description: raw.description,
        category: raw.category as TicketCategory,
        priority: raw.priority,
        equipmentId: raw.equipmentId ?? undefined,
        establishmentId: raw.establishmentId ?? undefined,
      })
      .subscribe({
        next: (ticket) => {
          this.toast.success(`Ticket ${ticket.reference} enregistre.`);
          void this.router.navigate(['/tickets', ticket.id]);
        },
        error: () => this.submitting.set(false),
      });
  }

  protected cancel(): void {
    void this.router.navigate(['/tickets']);
  }
}
