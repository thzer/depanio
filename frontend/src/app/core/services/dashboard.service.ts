import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Dashboard } from '../models/api.models';

/**
 * Indicateurs du tableau de bord.
 *
 * Un seul appel suffit : le serveur assemble tous les agregats, ce qui
 * garantit que les chiffres affiches correspondent au meme instant.
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  load(): Observable<Dashboard> {
    return this.http.get<Dashboard>(`${environment.apiUrl}/dashboard`);
  }
}
