import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DemoRequest, DemoRequestForm, Page } from '../models/api.models';

/**
 * Demandes de demonstration laissees depuis la page commerciale.
 *
 * Le depot est le seul appel de l application qui ne porte pas de jeton :
 * il vient d un visiteur qui, par definition, n a pas encore de compte.
 */
@Injectable({ providedIn: 'root' })
export class DemoRequestService {
  private readonly http = inject(HttpClient);

  submit(form: DemoRequestForm): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(
      `${environment.apiUrl}/public/demo-requests`,
      form,
    );
  }

  list(onlyPending = false, page = 0, size = 30): Observable<Page<DemoRequest>> {
    return this.http.get<Page<DemoRequest>>(`${environment.apiUrl}/demo-requests`, {
      params: new HttpParams()
        .set('onlyPending', onlyPending)
        .set('page', page)
        .set('size', size),
    });
  }

  countPending(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(
      `${environment.apiUrl}/demo-requests/pending-count`,
    );
  }

  markHandled(id: number): Observable<DemoRequest> {
    return this.http.patch<DemoRequest>(
      `${environment.apiUrl}/demo-requests/${id}/handled`,
      {},
    );
  }
}
