import { Component, computed, input } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { inject } from '@angular/core';

/**
 * Jeu d icones vectorielles de l application.
 *
 * Les traces sont integrees au code plutot que chargees depuis une
 * bibliotheque externe : aucune requete reseau supplementaire, aucune
 * dependance a maintenir, et les icones heritent de la couleur du texte
 * environnant grace a currentColor.
 *
 * Toutes sont dessinees sur une grille de 24 pixels, en traits de 1,75
 * pixel, pour un rendu homogene.
 */
const PATHS: Record<string, string> = {
  dashboard:
    '<rect x="3" y="3" width="7" height="9" rx="1.5"/>' +
    '<rect x="14" y="3" width="7" height="5" rx="1.5"/>' +
    '<rect x="14" y="12" width="7" height="9" rx="1.5"/>' +
    '<rect x="3" y="16" width="7" height="5" rx="1.5"/>',

  ticket:
    '<path d="M3 9.5V7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v2.5a2.5 2.5 0 0 0 0 5V17a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-2.5a2.5 2.5 0 0 0 0-5Z"/>' +
    '<path d="M14 5v2M14 11v2M14 17v2"/>',

  tool:
    '<path d="M14.7 6.3a4 4 0 0 0 5 5l-9.3 9.3a2.1 2.1 0 0 1-3-3Z"/>' +
    '<path d="M17.5 3.5 20.5 6.5"/>' +
    '<circle cx="6.5" cy="6.5" r="3"/>',

  store:
    '<path d="M3 9.5 4.5 4h15L21 9.5"/>' +
    '<path d="M3 9.5a2.5 2.5 0 0 0 5 0 2.5 2.5 0 0 0 5 0 2.5 2.5 0 0 0 5 0 2.5 2.5 0 0 0 3 0"/>' +
    '<path d="M4.5 11.5V20h15v-8.5"/>' +
    '<path d="M9.5 20v-5h5v5"/>',

  users:
    '<circle cx="9" cy="8" r="3.2"/>' +
    '<path d="M3.5 20a5.5 5.5 0 0 1 11 0"/>' +
    '<path d="M16 5.5a3.2 3.2 0 0 1 0 5"/>' +
    '<path d="M17.5 14.5a5.5 5.5 0 0 1 3 5"/>',

  bell:
    '<path d="M18 9a6 6 0 1 0-12 0c0 5-2 6.5-2 6.5h16S18 14 18 9Z"/>' +
    '<path d="M10.3 19a2 2 0 0 0 3.4 0"/>',

  plus: '<path d="M12 5v14M5 12h14"/>',

  search: '<circle cx="11" cy="11" r="6.5"/><path d="M20 20l-3.5-3.5"/>',

  menu: '<path d="M4 7h16M4 12h16M4 17h16"/>',

  close: '<path d="M6 6l12 12M18 6 6 18"/>',

  check: '<path d="m5 12.5 4.5 4.5L19 7.5"/>',

  clock: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',

  alert:
    '<path d="M12 3.5 21 19.5H3Z"/><path d="M12 10v4"/><circle cx="12" cy="17" r="0.6" fill="currentColor"/>',

  qr:
    '<rect x="3.5" y="3.5" width="6" height="6" rx="1"/>' +
    '<rect x="14.5" y="3.5" width="6" height="6" rx="1"/>' +
    '<rect x="3.5" y="14.5" width="6" height="6" rx="1"/>' +
    '<path d="M14.5 14.5h2.5v2.5h-2.5Z"/>' +
    '<path d="M20.5 14.5v2.5M17 20.5h3.5M14.5 20.5h.01"/>',

  logout: '<path d="M15 16.5V19a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h7a2 2 0 0 1 2 2v2.5"/><path d="M19.5 12H9.5m10 0-3-3m3 3-3 3"/>',

  chevronRight: '<path d="m9 5 7 7-7 7"/>',

  chevronLeft: '<path d="m15 5-7 7 7 7"/>',

  refresh:
    '<path d="M20 11a8 8 0 0 0-13.7-5.3L3.5 8.5"/>' +
    '<path d="M4 13a8 8 0 0 0 13.7 5.3l2.8-2.8"/>' +
    '<path d="M3.5 4v4.5H8M20.5 20v-4.5H16"/>',

  print:
    '<path d="M7 9V4h10v5"/>' +
    '<rect x="3.5" y="9" width="17" height="7" rx="2"/>' +
    '<path d="M7 14h10v6H7Z"/>',

  send: '<path d="M4.5 11.5 20 4.5l-7 15.5-2.5-6.5Z"/><path d="m10.5 13.5 9.5-9"/>',

  shield: '<path d="M12 3.5 19.5 6v6c0 4.5-3 7.5-7.5 9-4.5-1.5-7.5-4.5-7.5-9V6Z"/><path d="m9 12 2 2 4-4"/>',

  chart: '<path d="M4 20V4"/><path d="M4 20h16"/><path d="M8 20v-6M12.5 20V8M17 20v-9"/>',

  wifi: '<path d="M4 9a13 13 0 0 1 16 0"/><path d="M7 12.5a8.5 8.5 0 0 1 10 0"/><path d="M10 16a4 4 0 0 1 4 0"/><circle cx="12" cy="19" r="0.6" fill="currentColor"/>',
};

/**
 * Icone vectorielle.
 *
 * @example <app-icon name="ticket" [size]="18" />
 */
@Component({
  selector: 'app-icon',
  template: `<span class="icon" [style.width.px]="size()" [style.height.px]="size()" [innerHTML]="svg()"></span>`,
  styles: `
    .icon {
      display: inline-flex;
      flex-shrink: 0;
      line-height: 0;
    }

    .icon ::ng-deep svg {
      width: 100%;
      height: 100%;
      display: block;
    }
  `,
})
export class Icon {
  readonly name = input.required<string>();
  readonly size = input(20);

  private readonly sanitizer = inject(DomSanitizer);

  protected readonly svg = computed<SafeHtml>(() => {
    const path = PATHS[this.name()] ?? '';
    const markup =
      `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" ` +
      `stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" ` +
      `aria-hidden="true">${path}</svg>`;

    // Le contenu provient exclusivement de la table ci-dessus, jamais
    // d une saisie utilisateur : le marquer comme sur est donc legitime.
    return this.sanitizer.bypassSecurityTrustHtml(markup);
  });
}
