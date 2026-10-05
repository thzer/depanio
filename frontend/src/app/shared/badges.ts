import { Component, computed, input } from '@angular/core';
import { PRIORITY_CLASS, Priority, STATUS_CLASS, TicketStatus } from '../core/models/enums';

/**
 * Pastille de couleur indiquant le statut d'un ticket.
 *
 * La couleur est deduite du statut technique, le texte provient du libelle
 * calcule par le serveur : aucun libelle n'est recopie cote navigateur.
 */
@Component({
  selector: 'app-status-badge',
  template: `<span class="badge {{ cssClass() }}">{{ label() }}</span>`,
})
export class StatusBadge {
  readonly status = input.required<TicketStatus>();
  readonly label = input.required<string>();

  protected readonly cssClass = computed(() => STATUS_CLASS[this.status()]);
}

/**
 * Pastille de couleur indiquant la priorite d'un ticket.
 */
@Component({
  selector: 'app-priority-badge',
  template: `<span class="badge {{ cssClass() }}">{{ label() }}</span>`,
})
export class PriorityBadge {
  readonly priority = input.required<Priority>();
  readonly label = input.required<string>();

  protected readonly cssClass = computed(() => PRIORITY_CLASS[this.priority()]);
}

/**
 * Indicateur du delai contractuel restant.
 *
 * Trois etats sont distingues pour que l'information se lise d'un coup
 * d'oeil dans une liste : delai respecte, echeance proche, delai depasse.
 */
@Component({
  selector: 'app-sla-indicator',
  template: `
    @if (breached() || isOverdue()) {
      <span class="sla-breached" [title]="dueAt() ?? ''">
        Dépassé{{ lateText() }}
      </span>
    } @else if (hours() === undefined || hours() === null) {
      <span class="text-faint">&mdash;</span>
    } @else if (hours()! <= 2) {
      <span class="sla-soon" [title]="dueAt() ?? ''">{{ remainingText() }}</span>
    } @else {
      <span class="sla-ok" [title]="dueAt() ?? ''">{{ remainingText() }}</span>
    }
  `,
})
export class SlaIndicator {
  /** Heures restantes avant l'echeance ; negatif si elle est depassee. */
  readonly hours = input<number | null | undefined>();

  /** Depassement deja constaté par le serveur. */
  readonly breached = input(false);

  readonly dueAt = input<string | null | undefined>();

  protected readonly isOverdue = computed(() => {
    const value = this.hours();
    return value !== undefined && value !== null && value < 0;
  });

  protected readonly lateText = computed(() => {
    const value = this.hours();
    if (value === undefined || value === null || value >= 0) {
      return '';
    }
    return ` de ${this.format(Math.abs(value))}`;
  });

  /**
   * « 1 h restante », « 3 j restants », « moins d'une heure ».
   *
   * L'accord n'est pas un detail d'esthetique : un tableau qui affiche
   * « 1 h restantes » se lit comme un travail bacle, et c'est la premiere
   * chose que voit un client a qui l'on vend l'outil.
   */
  protected readonly remainingText = computed(() => {
    const value = this.hours();
    if (value === undefined || value === null) {
      return '';
    }

    const duree = this.format(value);
    if (duree.startsWith('moins')) {
      return duree;
    }

    const quantite = Number.parseInt(duree, 10);
    const jours = duree.endsWith('j');
    const accord = jours
      ? quantite > 1
        ? 'restants'
        : 'restant'
      : quantite > 1
        ? 'restantes'
        : 'restante';

    return `${duree} ${accord}`;
  });

  /** Au-dela de 48 heures, un nombre de jours se lit plus vite. */
  private format(hours: number): string {
    if (hours >= 48) {
      return `${Math.round(hours / 24)} j`;
    }
    if (hours < 1) {
      return 'moins d’une heure';
    }
    return `${Math.round(hours)} h`;
  }
}
