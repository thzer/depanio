import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Ajoute le jeton JWT a chaque appel sortant vers l'API.
 *
 * Centraliser cet ajout evite de le repeter dans chaque service, et
 * garantit qu'aucun appel ne partira par erreur sans authentification.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const token = inject(AuthService).getToken();

  // Les appels de connexion et de renouvellement ne portent pas de jeton
  const isAuthEndpoint =
    request.url.includes('/auth/login') || request.url.includes('/auth/refresh');

  if (!token || isAuthEndpoint) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    }),
  );
};
