import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreateUserRequest,
  Page,
  TechnicianWorkload,
  UpdateUserRequest,
  UserResponse,
} from '../models/api.models';
import { Role } from '../models/enums';

/** Appels relatifs a l'administration des comptes. */
@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/users`;

  search(search?: string, role?: Role, page = 0, size = 20): Observable<Page<UserResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search) params = params.set('search', search);
    if (role) params = params.set('role', role);

    return this.http.get<Page<UserResponse>>(this.baseUrl, { params });
  }

  /** Techniciens actifs, pour la liste deroulante affectation. */
  technicians(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`${this.baseUrl}/technicians`);
  }

  /** Techniciens classes du moins charge au plus charge. */
  workload(): Observable<TechnicianWorkload[]> {
    return this.http.get<TechnicianWorkload[]>(`${this.baseUrl}/technicians/workload`);
  }

  getById(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateUserRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(this.baseUrl, request);
  }

  update(id: number, request: UpdateUserRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.baseUrl}/${id}`, request);
  }

  setActive(id: number, active: boolean): Observable<UserResponse> {
    return this.http.patch<UserResponse>(
      `${this.baseUrl}/${id}/active`,
      {},
      { params: new HttpParams().set('active', active) },
    );
  }

  resetPassword(id: number, newPassword: string): Observable<void> {
    return this.http.patch<void>(
      `${this.baseUrl}/${id}/reset-password`,
      {},
      { params: new HttpParams().set('newPassword', newPassword) },
    );
  }
}
