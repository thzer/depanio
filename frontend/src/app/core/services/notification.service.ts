import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, effect, inject, signal } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { NotificationResponse, Page } from '../models/api.models';
import { AuthService } from './auth.service';
import { ToastService } from './toast.service';

/**
 * Notifications de l'utilisateur connecte.
 *
 * Le service combine deux sources complementaires :
 *
 * 1. **HTTP**, au demarrage, pour recuperer ce qui est arrive pendant que
 *    l'utilisateur etait absent ;
 * 2. **WebSocket (STOMP)**, ensuite, pour recevoir les nouvelles
 *    notifications sans interroger le serveur en boucle.
 *
 * La connexion temps reel suit la session : elle s'ouvre a la connexion et
 * se ferme a la deconnexion, grace a un effet qui observe le signal
 * d'authentification.
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  /** Destination personnelle resolue par le serveur pour chaque session. */
  private static readonly USER_QUEUE = '/user/queue/notifications';

  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly baseUrl = `${environment.apiUrl}/notifications`;

  private client?: Client;

  private readonly unread = signal(0);
  private readonly live = signal<NotificationResponse[]>([]);
  private readonly connected = signal(false);

  /** Nombre de notifications non lues, affiche sur la cloche. */
  readonly unreadCount = this.unread.asReadonly();

  /** Notifications recues en direct pendant cette session. */
  readonly liveNotifications = this.live.asReadonly();

  /** Etat du canal temps reel, affiche discretement dans l en-tete. */
  readonly isConnected = this.connected.asReadonly();

  constructor() {
    // La connexion temps reel suit l etat de la session
    effect(() => {
      if (this.auth.isAuthenticated()) {
        this.connect();
        this.refreshUnreadCount();
      } else {
        this.disconnect();
        this.unread.set(0);
        this.live.set([]);
      }
    });
  }

  // ---------------------------------------------------------------- HTTP

  list(page = 0, size = 20): Observable<Page<NotificationResponse>> {
    return this.http.get<Page<NotificationResponse>>(this.baseUrl, {
      params: new HttpParams().set('page', page).set('size', size),
    });
  }

  markAsRead(id: number): Observable<void> {
    return this.http
      .patch<void>(`${this.baseUrl}/${id}/read`, {})
      .pipe(tap(() => this.unread.update((count) => Math.max(0, count - 1))));
  }

  markAllAsRead(): Observable<{ updated: number }> {
    return this.http
      .patch<{ updated: number }>(`${this.baseUrl}/read-all`, {})
      .pipe(tap(() => this.unread.set(0)));
  }

  refreshUnreadCount(): void {
    this.http
      .get<{ count: number }>(`${this.baseUrl}/unread-count`)
      .subscribe({
        next: (response) => this.unread.set(response.count),
        // Un echec ici ne doit pas perturber l utilisateur :
        // le compteur sera simplement rafraichi au prochain essai
        error: () => undefined,
      });
  }

  // ----------------------------------------------------------- Temps reel

  /**
   * Ouvre le canal temps reel.
   *
   * Le jeton est transmis dans les en-tetes de la trame CONNECT : un
   * navigateur ne permet pas de poser un en-tete HTTP sur un handshake
   * WebSocket, STOMP offre donc le seul endroit ou passer l identite.
   */
  private connect(): void {
    if (this.client?.active) {
      return;
    }

    const token = this.auth.getToken();
    if (!token) {
      return;
    }

    this.client = new Client({
      brokerURL: environment.wsUrl,
      connectHeaders: { Authorization: `Bearer ${token}` },

      // Reconnexion automatique en cas de coupure reseau
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,

      onConnect: () => {
        this.connected.set(true);
        this.client?.subscribe(NotificationService.USER_QUEUE, (message: IMessage) =>
          this.handleIncoming(message),
        );
      },

      onDisconnect: () => this.connected.set(false),
      onWebSocketClose: () => this.connected.set(false),

      // Les erreurs du canal sont silencieuses pour l utilisateur :
      // l application reste utilisable sans notifications instantanees
      onStompError: () => this.connected.set(false),
    });

    this.client.activate();
  }

  private disconnect(): void {
    void this.client?.deactivate();
    this.client = undefined;
    this.connected.set(false);
  }

  private handleIncoming(message: IMessage): void {
    try {
      const notification = JSON.parse(message.body) as NotificationResponse;
      this.live.update((list) => [notification, ...list].slice(0, 50));
      this.unread.update((count) => count + 1);
      this.toast.info(notification.message);
    } catch {
      // Message illisible : on ignore plutot que de casser l affichage
    }
  }
}
