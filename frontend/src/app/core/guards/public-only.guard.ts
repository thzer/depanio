import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Reserve une page aux visiteurs non connectes.
 *
 * Applique a la page d accueil commerciale : un client deja connecte n a
 * aucune raison de revoir l argumentaire de vente, il est envoye
 * directement a son tableau de bord.
 */
export const publicOnlyGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.isAuthenticated() ? router.createUrlTree(['/tableau-de-bord']) : true;
};
