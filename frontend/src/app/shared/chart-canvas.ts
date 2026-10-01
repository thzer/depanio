import {
  Component,
  ElementRef,
  OnDestroy,
  effect,
  input,
  viewChild,
} from '@angular/core';
import {
  ArcElement,
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  ChartType,
  DoughnutController,
  Legend,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from 'chart.js';
import { ChartPoint } from '../core/models/api.models';

// Chart.js est modulaire : seuls les elements reellement utilises sont
// enregistres, ce qui evite d embarquer tous les types de graphiques.
Chart.register(
  BarController,
  BarElement,
  LineController,
  LineElement,
  PointElement,
  DoughnutController,
  ArcElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend,
);

/**
 * Graphique reutilisable, alimente par une liste de points.
 *
 * Le composant encapsule entierement Chart.js : les ecrans se contentent
 * de fournir des donnees, sans connaitre la bibliotheque. Le graphique est
 * reconstruit a chaque changement de donnees et detruit a la fermeture de
 * l ecran, ce qui evite les fuites de memoire sur le canevas.
 */
@Component({
  selector: 'app-chart-canvas',
  template: `
    <div class="chart-wrapper" [style.height.px]="height()">
      <canvas #canvas></canvas>
    </div>
  `,
  styles: `
    .chart-wrapper {
      position: relative;
      width: 100%;
    }
  `,
})
export class ChartCanvas implements OnDestroy {
  /** Palette appliquee aux graphiques circulaires et aux barres colorees. */
  private static readonly PALETTE = [
    '#1e5f8c',
    '#b3261e',
    '#b06d00',
    '#1f7a4d',
    '#5e35b1',
    '#0b7285',
    '#c2410c',
    '#495057',
  ];

  readonly data = input.required<ChartPoint[]>();
  readonly type = input<ChartType>('bar');
  readonly height = input(260);
  readonly label = input('Tickets');

  /** Affiche la legende, utile pour un graphique circulaire. */
  readonly showLegend = input(false);

  private readonly canvasRef =
    viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');

  private chart?: Chart;

  constructor() {
    effect(() => {
      const points = this.data();
      const type = this.type();
      this.render(points, type);
    });
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  private render(points: ChartPoint[], type: ChartType): void {
    const canvas = this.canvasRef().nativeElement;

    // Une instance existante doit etre detruite : Chart.js refuse de
    // reutiliser un canevas deja occupe
    this.chart?.destroy();

    const colors = points.map(
      (_, index) => ChartCanvas.PALETTE[index % ChartCanvas.PALETTE.length],
    );
    const isCircular = type === 'doughnut' || type === 'pie';

    this.chart = new Chart(canvas, {
      type,
      data: {
        labels: points.map((point) => point.label),
        datasets: [
          {
            label: this.label(),
            data: points.map((point) => point.value),
            backgroundColor: isCircular ? colors : 'rgba(30, 95, 140, 0.82)',
            borderColor: isCircular ? '#fff' : '#1e5f8c',
            borderWidth: isCircular ? 2 : 1,
            borderRadius: type === 'bar' ? 5 : 0,
            fill: type === 'line' ? false : undefined,
            tension: type === 'line' ? 0.32 : undefined,
            pointRadius: type === 'line' ? 3 : undefined,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: this.showLegend(),
            position: 'bottom',
            labels: { boxWidth: 12, padding: 14, font: { size: 11 } },
          },
          tooltip: {
            padding: 10,
            displayColors: false,
          },
        },
        scales: isCircular
          ? undefined
          : {
              x: {
                grid: { display: false },
                ticks: { font: { size: 11 } },
              },
              y: {
                beginAtZero: true,
                // Les effectifs sont des entiers : on supprime les
                // graduations decimales qui n ont pas de sens ici
                ticks: { precision: 0, font: { size: 11 } },
                grid: { color: 'rgba(0, 0, 0, 0.055)' },
              },
            },
      },
    });
  }
}
