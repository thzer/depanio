import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EstablishmentRequest, EstablishmentResponse, Page } from '../models/api.models';
import { EstablishmentType } from '../models/enums';

/** Appels relatifs aux etablissements clients. */
@Injectable({ providedIn: 'root' })
export class EstablishmentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/establishments`;

  search(
    search?: string,
    type?: EstablishmentType,
    active?: boolean,
    page = 0,
    size = 20,
  ): Observable<Page<EstablishmentResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search) params = params.set('search', search);
    if (type) params = params.set('type', type);
    if (active !== undefined) params = params.set('active', active);

    return this.http.get<Page<EstablishmentResponse>>(this.baseUrl, { params });
  }

  /** Etablissements actifs, pour les listes deroulantes. */
  active(): Observable<EstablishmentResponse[]> {
    return this.http.get<EstablishmentResponse[]>(`${this.baseUrl}/active`);
  }

  getById(id: number): Observable<EstablishmentResponse> {
    return this.http.get<EstablishmentResponse>(`${this.baseUrl}/${id}`);
  }

  create(request: EstablishmentRequest): Observable<EstablishmentResponse> {
    return this.http.post<EstablishmentResponse>(this.baseUrl, request);
  }

  update(id: number, request: EstablishmentRequest): Observable<EstablishmentResponse> {
    return this.http.put<EstablishmentResponse>(`${this.baseUrl}/${id}`, request);
  }

  setActive(id: number, active: boolean): Observable<EstablishmentResponse> {
    return this.http.patch<EstablishmentResponse>(
      `${this.baseUrl}/${id}/active`,
      {},
      { params: new HttpParams().set('active', active) },
    );
  }
}
