import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ChangeStatusRequest,
  CreateTicketRequest,
  Page,
  TicketComment,
  TicketDetail,
  TicketSearchParams,
  TicketSummary,
  UpdateTicketRequest,
} from '../models/api.models';

/**
 * Appels relatifs aux tickets.
 *
 * Aucune regle metier n'est dupliquee ici : le serveur reste seul juge des
 * transitions autorisees et du perimetre de chaque utilisateur. Ce service
 * se limite a traduire les actions de l'interface en appels HTTP.
 */
@Injectable({ providedIn: 'root' })
export class TicketService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/tickets`;

  search(params: TicketSearchParams = {}): Observable<Page<TicketSummary>> {
    return this.http.get<Page<TicketSummary>>(this.baseUrl, {
      params: this.toHttpParams(params),
    });
  }

  myAssigned(onlyOpen = true, page = 0, size = 20): Observable<Page<TicketSummary>> {
    return this.http.get<Page<TicketSummary>>(`${this.baseUrl}/my-assigned`, {
      params: this.toHttpParams({ onlyOpen, page, size }),
    });
  }

  unassigned(): Observable<TicketSummary[]> {
    return this.http.get<TicketSummary[]>(`${this.baseUrl}/unassigned`);
  }

  getById(id: number): Observable<TicketDetail> {
    return this.http.get<TicketDetail>(`${this.baseUrl}/${id}`);
  }

  getByReference(reference: string): Observable<TicketDetail> {
    return this.http.get<TicketDetail>(`${this.baseUrl}/by-reference/${reference}`);
  }

  create(request: CreateTicketRequest): Observable<TicketDetail> {
    return this.http.post<TicketDetail>(this.baseUrl, request);
  }

  /** Declaration depuis une etiquette QR : le materiel est deduit du jeton. */
  createFromQr(qrToken: string, request: CreateTicketRequest): Observable<TicketDetail> {
    return this.http.post<TicketDetail>(`${this.baseUrl}/from-qr/${qrToken}`, request);
  }

  update(id: number, request: UpdateTicketRequest): Observable<TicketDetail> {
    return this.http.put<TicketDetail>(`${this.baseUrl}/${id}`, request);
  }

  assign(id: number, technicianId: number): Observable<TicketDetail> {
    return this.http.patch<TicketDetail>(`${this.baseUrl}/${id}/assign`, { technicianId });
  }

  changeStatus(id: number, request: ChangeStatusRequest): Observable<TicketDetail> {
    return this.http.patch<TicketDetail>(`${this.baseUrl}/${id}/status`, request);
  }

  addComment(id: number, content: string, internalNote = false): Observable<TicketComment> {
    return this.http.post<TicketComment>(`${this.baseUrl}/${id}/comments`, {
      content,
      internalNote,
    });
  }

  /**
   * Construit les parametres de requete en ignorant les valeurs absentes.
   * Un filtre non renseigne ne doit pas etre envoye : le serveur le
   * traiterait comme une chaine vide au lieu de desactiver le critere.
   */
  private toHttpParams(source: object): HttpParams {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(source)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return params;
  }
}
