import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Dashboard } from '../../core/models/api.models';
import { AuthService } from '../../core/services/auth.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { PriorityBadge, SlaIndicator, StatusBadge } from '../../shared/badges';
import { ChartCanvas } from '../../shared/chart-canvas';

/**
 * Tableau de bord.
 *
 * Toutes les donnees proviennent d'un seul appel : les chiffres affiches
 * correspondent donc au meme instant de lecture, ce qui ne serait pas
 * garanti avec plusieurs appels paralleles.
 *
 * Le contenu s'adapte au role : un gerant ne voit que son etablissement,
 * et la repartition par technicien lui est masquee puisqu'elle concerne
 * l'organisation interne de la societe de maintenance.
 */
@Component({
  selector: 'app-dashboard',
  imports: [
    RouterLink,
    DatePipe,
    DecimalPipe,
    ChartCanvas,
    StatusBadge,
    PriorityBadge,
    SlaIndicator,
  ],
  templateUrl: './dashboard.html',
})
export class DashboardPage {
  private readonly dashboardService = inject(DashboardService);
  protected readonly auth = inject(AuthService);

  protected readonly data = signal<Dashboard | null>(null);
  protected readonly loading = signal(true);

  /** Horodatage de la derniere lecture, affiche en bas de page. */
  protected readonly now = signal(new Date());

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.dashboardService.load().subscribe({
      next: (dashboard) => {
        this.data.set(dashboard);
        this.now.set(new Date());
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
