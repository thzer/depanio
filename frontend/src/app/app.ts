import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastContainer } from './shared/toast-container';

/**
 * Racine de l'application.
 *
 * Elle ne contient que la zone de navigation et la pile de messages :
 * tout le reste est fourni par les ecrans charges dynamiquement.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastContainer],
  template: `
    <router-outlet />
    <app-toast-container />
  `,
})
export class App {}
