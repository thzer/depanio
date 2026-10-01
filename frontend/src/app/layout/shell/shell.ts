import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';

interface NavItem {
  path: string;
  label: string;
  icon: string;
  /** Roles autorises ; vide signifie accessible a tous les roles. */
  roles?: ('ADMIN' | 'TECHNICIAN' | 'CLIENT')[];
}

/**
 * Cadre commun a tous les ecrans internes : menu lateral, barre
 * superieure et zone de contenu.
 *
 * Le menu est filtre selon le role de l'utilisateur. Ce filtrage est un
 * confort d'affichage : les memes restrictions sont appliquees par les
 * gardes de route et, de maniere definitive, par le serveur.
 */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  protected readonly auth = inject(AuthService);
  protected readonly notifications = inject(NotificationService);
  protected readonly appName = environment.appName;

  protected readonly sidebarOpen = signal(false);
  protected readonly profileOpen = signal(false);

  private readonly allItems: NavItem[] = [
    { path: '/tableau-de-bord', label: 'Tableau de bord', icon: 'grid' },
    { path: '/tickets', label: 'Tickets', icon: 'ticket' },
    { path: '/materiel', label: 'Materiel', icon: 'tool' },
    {
      path: '/etablissements',
      label: 'Etablissements',
      icon: 'store',
      roles: ['ADMIN', 'TECHNICIAN'],
    },
    { path: '/utilisateurs', label: 'Utilisateurs', icon: 'users', roles: ['ADMIN'] },
  ];

  /** Entrees de menu visibles pour le role courant. */
  protected readonly navItems = computed(() =>
    this.allItems.filter((item) => !item.roles || this.auth.hasAnyRole(...item.roles)),
  );

  /** Les gerants declarent eux-memes leurs pannes depuis le menu. */
  protected readonly canCreateTicket = computed(() => this.auth.isAuthenticated());

  protected toggleSidebar(): void {
    this.sidebarOpen.update((open) => !open);
  }

  protected closeSidebar(): void {
    this.sidebarOpen.set(false);
  }

  protected toggleProfile(): void {
    this.profileOpen.update((open) => !open);
  }

  protected logout(): void {
    this.profileOpen.set(false);
    this.auth.logout();
  }
}
