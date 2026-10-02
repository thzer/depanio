import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  EstablishmentResponse,
  Page,
  TechnicianWorkload,
  UserResponse,
} from '../../core/models/api.models';
import { EnumOption, Role } from '../../core/models/enums';
import { AuthService } from '../../core/services/auth.service';
import { EstablishmentService } from '../../core/services/establishment.service';
import { ReferenceService } from '../../core/services/reference.service';
import { ToastService } from '../../core/services/toast.service';
import { UserService } from '../../core/services/user.service';
import { Icon } from '../../shared/icon';
import { Modal } from '../../shared/modal';

/**
 * Administration des comptes utilisateurs.
 *
 * La charge de travail des techniciens est affichee a cote de la liste :
 * elle aide a repartir les affectations sans changer d ecran.
 */
@Component({
  selector: 'app-user-list',
  imports: [FormsModule, ReactiveFormsModule, Icon, Modal],
  templateUrl: './user-list.html',
  styles: `
    .layout {
      display: grid;
      grid-template-columns: 1fr 300px;
      gap: var(--sp-4);
      align-items: start;
    }

    .workload {
      display: flex;
      align-items: center;
      gap: var(--sp-3);
      padding: var(--sp-3) 0;
      border-bottom: 1px solid var(--ink-100);
    }

    .workload:last-child {
      border-bottom: none;
    }

    .workload__count {
      display: grid;
      place-items: center;
      width: 34px;
      height: 34px;
      border-radius: 50%;
      background: var(--brand-50);
      color: var(--brand-700);
      font-weight: 700;
      flex-shrink: 0;
    }

    .form-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: var(--sp-4);
    }

    .form-grid__full {
      grid-column: 1 / -1;
    }

    @media (max-width: 1000px) {
      .layout {
        grid-template-columns: 1fr;
      }
    }

    @media (max-width: 700px) {
      .form-grid {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class UserListPage {
  private readonly userService = inject(UserService);
  private readonly establishmentService = inject(EstablishmentService);
  private readonly referenceService = inject(ReferenceService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  protected readonly auth = inject(AuthService);

  protected readonly page = signal<Page<UserResponse> | null>(null);
  protected readonly workload = signal<TechnicianWorkload[]>([]);
  protected readonly roles = signal<EnumOption[]>([]);
  protected readonly establishments = signal<EstablishmentResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly formOpen = signal(false);

  protected search = '';
  protected role: Role | '' = '';

  protected readonly form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(10)]],
    role: ['TECHNICIAN' as Role, [Validators.required]],
    phone: [''],
    speciality: [''],
    establishmentId: [null as number | null],
  });

  constructor() {
    this.referenceService.all().subscribe((reference) => this.roles.set(reference.roles));
    this.establishmentService.active().subscribe((list) => this.establishments.set(list));
    this.load();
    this.loadWorkload();
  }

  protected load(): void {
    this.loading.set(true);
    this.userService.search(this.search || undefined, this.role || undefined, 0, 50).subscribe({
      next: (result) => {
        this.page.set(result);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  private loadWorkload(): void {
    this.userService.workload().subscribe((list) => this.workload.set(list));
  }

  protected resetFilters(): void {
    this.search = '';
    this.role = '';
    this.load();
  }

  // ------------------------------------------------------------- Creation

  protected openCreate(): void {
    this.form.reset({ role: 'TECHNICIAN', establishmentId: null });
    this.formOpen.set(true);
  }

  protected closeForm(): void {
    this.formOpen.set(false);
  }

  /** Le role choisi determine les champs encore pertinents. */
  protected get selectedRole(): Role {
    return this.form.controls.role.value;
  }

  protected submit(): void {
    const raw = this.form.getRawValue();

    // Un compte client doit etre rattache a un etablissement : la regle
    // est verifiee ici pour expliquer le probleme sur le champ concerne,
    // et de nouveau par le serveur qui reste seul juge.
    if (raw.role === 'CLIENT' && !raw.establishmentId) {
      this.form.controls.establishmentId.setErrors({ required: true });
    }

    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.userService
      .create({
        fullName: raw.fullName.trim(),
        email: raw.email.trim(),
        password: raw.password,
        role: raw.role,
        phone: raw.phone || undefined,
        speciality: raw.role === 'TECHNICIAN' ? raw.speciality || undefined : undefined,
        establishmentId: raw.role === 'CLIENT' ? (raw.establishmentId ?? undefined) : undefined,
      })
      .subscribe({
        next: (created) => {
          this.saving.set(false);
          this.formOpen.set(false);
          this.toast.success(
            `Le compte de ${created.fullName} a ete cree. Communiquez-lui son mot de passe.`,
          );
          this.load();
          this.loadWorkload();
        },
        error: () => this.saving.set(false),
      });
  }

  /**
   * Active ou desactive un compte.
   * Le serveur refuse de desactiver le dernier administrateur, ainsi qu un
   * technicien qui a encore des tickets en cours.
   */
  protected toggleActive(user: UserResponse): void {
    this.userService.setActive(user.id, !user.active).subscribe({
      next: (updated) => {
        this.toast.success(
          updated.active
            ? `Le compte de ${updated.fullName} est reactive.`
            : `Le compte de ${updated.fullName} est desactive.`,
        );
        this.load();
        this.loadWorkload();
      },
      error: () => undefined,
    });
  }
}
