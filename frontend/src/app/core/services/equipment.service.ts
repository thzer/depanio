import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  EquipmentRequest,
  EquipmentResponse,
  MostProblematicEquipment,
  Page,
} from '../models/api.models';
import { EquipmentType } from '../models/enums';

/** Appels relatifs au parc de materiel et aux etiquettes QR. */
@Injectable({ providedIn: 'root' })
export class EquipmentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/equipments`;

  search(
    establishmentId?: number,
    type?: EquipmentType,
    search?: string,
    page = 0,
    size = 20,
  ): Observable<Page<EquipmentResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (establishmentId) params = params.set('establishmentId', establishmentId);
    if (type) params = params.set('type', type);
    if (search) params = params.set('search', search);

    return this.http.get<Page<EquipmentResponse>>(this.baseUrl, { params });
  }

  byEstablishment(establishmentId: number): Observable<EquipmentResponse[]> {
    return this.http.get<EquipmentResponse[]>(
      `${this.baseUrl}/by-establishment/${establishmentId}`,
    );
  }

  getById(id: number): Observable<EquipmentResponse> {
    return this.http.get<EquipmentResponse>(`${this.baseUrl}/${id}`);
  }

  /** Identifie un materiel apres lecture de son etiquette QR. */
  byQrToken(qrToken: string): Observable<EquipmentResponse> {
    return this.http.get<EquipmentResponse>(`${this.baseUrl}/by-qr/${qrToken}`);
  }

  /**
   * Recupere l'image PNG de l'etiquette sous forme binaire.
   *
   * Un appel dedie est necessaire parce que cette image est protegee : une
   * balise img classique ne transmettrait pas le jeton d'authentification,
   * et le serveur repondrait 401.
   */
  qrCodeBlob(id: number, size = 300): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${id}/qr-code`, {
      params: new HttpParams().set('size', size),
      responseType: 'blob',
    });
  }

  mostProblematic(
    establishmentId?: number,
    limit = 5,
  ): Observable<MostProblematicEquipment[]> {
    let params = new HttpParams().set('limit', limit);
    if (establishmentId) params = params.set('establishmentId', establishmentId);

    return this.http.get<MostProblematicEquipment[]>(`${this.baseUrl}/most-problematic`, {
      params,
    });
  }

  create(request: EquipmentRequest): Observable<EquipmentResponse> {
    return this.http.post<EquipmentResponse>(this.baseUrl, request);
  }

  update(id: number, request: EquipmentRequest): Observable<EquipmentResponse> {
    return this.http.put<EquipmentResponse>(`${this.baseUrl}/${id}`, request);
  }

  setActive(id: number, active: boolean): Observable<EquipmentResponse> {
    return this.http.patch<EquipmentResponse>(
      `${this.baseUrl}/${id}/active`,
      {},
      { params: new HttpParams().set('active', active) },
    );
  }
}
