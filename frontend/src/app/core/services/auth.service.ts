import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  ChangePasswordRequest,
  LoginRequest,
  UserResponse,
} from '../models/api.models';
import { Role } from '../models/enums';

/**
 * Session de l'utilisateur connecte.
 *
 * L'etat est expose sous forme de signaux : tout composant qui lit
 * {@link currentUser} se reaffiche automatiquement a la connexion comme a
 * la deconnexion, sans abonnement a gerer ni desabonnement a oublier.
 *
 * Le jeton est conserve dans le stockage local afin que la session
 * survive au rechargement de la page. Ce choix est assume : il expose le
 * jeton a un script malveillant injecte dans la page, mais permet une
 * API sans etat et sans cookie, donc sans protection CSRF a mettre en
 * place. Le jeton a une duree de vie limitee et ne contient aucune donnee
 * sensible.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private static readonly TOKEN_KEY = 'helpdesk.accessToken';
  private static readonly REFRESH_KEY = 'helpdesk.refreshToken';
  private static readonly USER_KEY = 'helpdesk.user';

  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly userState = signal<UserResponse | null>(this.readStoredUser());

  /** Utilisateur connecte, ou null si la session est fermee. */
  readonly currentUser = this.userState.asReadonly();

  readonly isAuthenticated = computed(() => this.userState() !== null);
  readonly isAdmin = computed(() => this.userState()?.role === 'ADMIN');
  readonly isTechnician = computed(() => this.userState()?.role === 'TECHNICIAN');
  readonly isClient = computed(() => this.userState()?.role === 'CLIENT');

  /** Initiales affichees dans la pastille du profil. */
  readonly initials = computed(() => {
    const name = this.userState()?.fullName ?? '';
    return name
      .split(' ')
      .filter((part) => part.length > 0)
      .slice(0, 2)
      .map((part) => part[0].toUpperCase())
      .join('');
  });

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/login`, credentials)
      .pipe(tap((response) => this.storeSession(response)));
  }

  /**
   * Ferme la session et renvoie vers l'ecran de connexion.
   * Appele a la demande de l'utilisateur comme a l'expiration du jeton.
   */
  logout(): void {
    localStorage.removeItem(AuthService.TOKEN_KEY);
    localStorage.removeItem(AuthService.REFRESH_KEY);
    localStorage.removeItem(AuthService.USER_KEY);
    this.userState.set(null);
    void this.router.navigate(['/login']);
  }

  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/auth/change-password`, request);
  }

  /** Recharge le profil depuis le serveur, par exemple apres une modification. */
  refreshProfile(): Observable<UserResponse> {
    return this.http
      .get<UserResponse>(`${environment.apiUrl}/auth/me`)
      .pipe(tap((user) => this.storeUser(user)));
  }

  getToken(): string | null {
    return localStorage.getItem(AuthService.TOKEN_KEY);
  }

  /** Verifie si l'utilisateur possede l'un des roles attendus. */
  hasAnyRole(...roles: Role[]): boolean {
    const role = this.userState()?.role;
    return role !== undefined && roles.includes(role);
  }

  private storeSession(response: AuthResponse): void {
    localStorage.setItem(AuthService.TOKEN_KEY, response.accessToken);
    localStorage.setItem(AuthService.REFRESH_KEY, response.refreshToken);
    this.storeUser(response.user);
  }

  private storeUser(user: UserResponse): void {
    localStorage.setItem(AuthService.USER_KEY, JSON.stringify(user));
    this.userState.set(user);
  }

  /**
   * Relit la session au demarrage de l'application.
   * Un stockage illisible est traite comme une absence de session plutot
   * que de faire echouer le demarrage.
   */
  private readStoredUser(): UserResponse | null {
    const token = localStorage.getItem(AuthService.TOKEN_KEY);
    const raw = localStorage.getItem(AuthService.USER_KEY);
    if (!token || !raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as UserResponse;
    } catch {
      localStorage.removeItem(AuthService.USER_KEY);
      return null;
    }
  }
}
