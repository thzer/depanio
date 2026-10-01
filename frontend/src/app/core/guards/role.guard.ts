import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Role } from '../models/enums';
import { AuthService } from '../services/auth.service';

/**
 * Restreint un ecran a certains roles.
 *
 * Ce controle n'est qu'un confort d'interface : il evite d'afficher un
 * ecran inutilisable. La veritable protection reste cote serveur, ou les
 * memes regles sont appliquees a chaque appel.
 *
 * @param roles roles autorises a ouvrir l'ecran
 */
export function roleGuard(...roles: Role[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (auth.hasAnyRole(...roles)) {
      return true;
    }

    return router.createUrlTree(['/tableau-de-bord']);
  };
}
