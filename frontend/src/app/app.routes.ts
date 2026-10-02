import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { publicOnlyGuard } from './core/guards/public-only.guard';
import { roleGuard } from './core/guards/role.guard';

/**
 * Plan de navigation de l'application.
 *
 * Chaque ecran est charge a la demande (import dynamique) : le navigateur
 * ne telecharge pas l'administration des comptes pour un gerant de cafe
 * qui n'y aura jamais acces.
 *
 * Les adresses sont en francais car l'application est destinee a des
 * utilisateurs francophones : une URL lisible aide a se reperer et a
 * partager un lien.
 */
export const routes: Routes = [
  {
    // Vitrine commerciale : premiere page vue par un visiteur.
    // Un utilisateur deja connecte est renvoye a son tableau de bord.
    path: '',
    pathMatch: 'full',
    title: 'Depanio — Assistance technique pour cafes et restaurants',
    canActivate: [publicOnlyGuard],
    loadComponent: () => import('./features/landing/landing').then((m) => m.LandingPage),
  },

  {
    path: 'login',
    title: 'Connexion — Depanio',
    loadComponent: () => import('./features/login/login').then((m) => m.LoginPage),
  },

  {
    path: '',
    loadComponent: () => import('./layout/shell/shell').then((m) => m.Shell),
    canActivate: [authGuard],
    children: [
      {
        path: 'tableau-de-bord',
        title: 'Tableau de bord',
        loadComponent: () =>
          import('./features/dashboard/dashboard').then((m) => m.DashboardPage),
      },

      {
        path: 'tickets',
        title: 'Tickets',
        loadComponent: () =>
          import('./features/tickets/ticket-list').then((m) => m.TicketListPage),
      },
      {
        path: 'tickets/nouveau',
        title: 'Nouveau ticket',
        loadComponent: () =>
          import('./features/tickets/ticket-create').then((m) => m.TicketCreatePage),
      },
      {
        path: 'tickets/:id',
        title: 'Detail du ticket',
        loadComponent: () =>
          import('./features/tickets/ticket-detail').then((m) => m.TicketDetailPage),
      },

      {
        path: 'materiel',
        title: 'Materiel',
        loadComponent: () =>
          import('./features/equipment/equipment-list').then((m) => m.EquipmentListPage),
      },

      {
        path: 'etablissements',
        title: 'Etablissements',
        canActivate: [roleGuard('ADMIN', 'TECHNICIAN')],
        loadComponent: () =>
          import('./features/establishments/establishment-list').then(
            (m) => m.EstablishmentListPage,
          ),
      },

      {
        path: 'utilisateurs',
        title: 'Utilisateurs',
        canActivate: [roleGuard('ADMIN')],
        loadComponent: () => import('./features/users/user-list').then((m) => m.UserListPage),
      },

      {
        // Adresse ouverte par la lecture d'une etiquette QR
        path: 'scan/:qrToken',
        title: 'Declarer une panne',
        loadComponent: () => import('./features/scan/scan').then((m) => m.ScanPage),
      },

      {
        path: 'notifications',
        title: 'Notifications',
        loadComponent: () =>
          import('./features/dashboard/notification-list').then((m) => m.NotificationListPage),
      },

      {
        path: 'mot-de-passe',
        title: 'Mot de passe',
        loadComponent: () =>
          import('./features/profile/change-password').then((m) => m.ChangePasswordPage),
      },
    ],
  },

  { path: '**', redirectTo: '' },
];
