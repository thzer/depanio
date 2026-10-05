import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

/**
 * Traitement unique des erreurs de l'API.
 *
 * Le backend renvoie toujours la meme structure d'erreur, ce qui permet de
 * la traduire ici en message affichable. Les composants n'ont donc pas a
 * gerer eux-memes l'affichage des erreurs techniques.
 */
export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const toast = inject(ToastService);

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      // Une erreur d'authentification sur la connexion elle-même est
      // affichee par le formulaire : inutile de la doubler d'une alerte
      const isLogin = request.url.includes('/auth/login');

      if (error.status === 0) {
        toast.error('Serveur injoignable. Vérifiez que l’API est démarrée.');
      } else if (error.status === 401 && !isLogin) {
        // Jeton expire ou revoque : la session est fermee
        toast.error('Votre session a expiré. Veuillez vous reconnecter.');
        auth.logout();
      } else if (error.status === 403) {
        toast.error('Vous n’avez pas les droits nécessaires pour cette action.');
      } else if (!isLogin) {
        const message = error.error?.message ?? 'Une erreur est survenue.';
        toast.error(message);
      }

      return throwError(() => error);
    }),
  );
};
