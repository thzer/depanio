import { Component, input } from '@angular/core';

/**
 * Logo de Depanio.
 *
 * Le symbole associe deux idees : la **coche** d un incident resolu et la
 * **bulle** d une demande declaree. Il est dessine en SVG afin de rester
 * net a toutes les tailles, de l icone du navigateur a l enseigne
 * imprimee, et de s adapter au fond clair comme au fond sombre.
 */
@Component({
  selector: 'app-logo',
  template: `
    <span class="logo" [class.logo--light]="light()">
      <svg
        class="logo__mark"
        [style.width.px]="size()"
        [style.height.px]="size()"
        viewBox="0 0 40 40"
        fill="none"
        aria-hidden="true"
      >
        <rect width="40" height="40" rx="11" [attr.fill]="light() ? '#ffffff' : 'var(--brand-600)'" />
        <path
          d="M11 20.8 16.6 26.4 29 14"
          [attr.stroke]="light() ? 'var(--brand-600)' : '#ffffff'"
          stroke-width="3.4"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
        <circle
          cx="29.5"
          cy="25.5"
          r="3.2"
          [attr.fill]="light() ? 'var(--accent-500)' : 'var(--accent-400)'"
        />
      </svg>

      @if (showText()) {
        <span class="logo__text">
          <span class="logo__name">Depanio</span>
          @if (showTagline()) {
            <span class="logo__tagline">Assistance technique</span>
          }
        </span>
      }
    </span>
  `,
  styles: `
    .logo {
      display: inline-flex;
      align-items: center;
      gap: 10px;
    }

    .logo__mark {
      flex-shrink: 0;
    }

    .logo__text {
      display: flex;
      flex-direction: column;
      line-height: 1.15;
    }

    .logo__name {
      font-weight: 700;
      font-size: 1.05rem;
      letter-spacing: -0.02em;
      color: var(--ink-900);
    }

    .logo--light .logo__name {
      color: #fff;
    }

    .logo__tagline {
      font-size: 0.68rem;
      letter-spacing: 0.04em;
      text-transform: uppercase;
      color: var(--ink-400);
    }

    .logo--light .logo__tagline {
      color: rgba(255, 255, 255, 0.5);
    }
  `,
})
export class Logo {
  /** Cote du symbole en pixels. */
  readonly size = input(36);

  /** Variante destinee a un fond sombre. */
  readonly light = input(false);

  readonly showText = input(true);
  readonly showTagline = input(true);
}
