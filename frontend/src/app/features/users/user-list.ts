import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Page, TechnicianWorkload, UserResponse } from '../../core/models/api.models';
import { EnumOption, Role } from '../../core/models/enums';
import { ReferenceService } from '../../core/services/reference.service';
import { ToastService } from '../../core/services/toast.service';
import { UserService } from '../../core/services/user.service';

/**
 * Administration des comptes utilisateurs.
 *
 * La charge de travail des techniciens est affichee a cote de la liste :
 * elle aide l administrateur a repartir les affectations sans ouvrir un
 * autre ecran.
 */
@Component({
  selector: 'app-user-list',
  imports: [FormsModule],
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
      border-bottom: 1px solid var(--gray-100);
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
      background: var(--primary-light);
      color: var(--primary-dark);
      font-weight: 700;
      flex-shrink: 0;
    }

    @media (max-width: 1000px) {
      .layout {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class UserListPage {
  private readonly userService = inject(UserService);
  private readonly referenceService = inject(ReferenceService);
  private readonly toast = inject(ToastService);

  protected readonly page = signal<Page<UserResponse> | null>(null);
  protected readonly workload = signal<TechnicianWorkload[]>([]);
  protected readonly roles = signal<EnumOption[]>([]);
  protected readonly loading = signal(true);

  protected search = '';
  protected role: Role | '' = '';

  constructor() {
    this.referenceService.all().subscribe((reference) => this.roles.set(reference.roles));
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
