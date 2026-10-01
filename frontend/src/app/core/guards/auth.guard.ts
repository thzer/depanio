import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Interdit l'acces aux ecrans internes sans session ouverte.
 *
 * L'adresse demandee est transmise a l'ecran de connexion : apres
 * authentification, l'utilisateur est renvoye la ou il voulait aller.
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/login'], {
    queryParams: { redirect: state.url },
  });
};
