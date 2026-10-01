/**
 * Configuration de l'environnement de developpement.
 *
 * L'adresse de l'API est centralisee ici : elle n'est jamais ecrite en dur
 * dans les services, ce qui permet de changer de serveur sans toucher au
 * reste du code.
 */
export const environment = {
  production: false,


  /** Racine de l'API REST. */
  apiUrl: 'http://localhost:8081/api',

  /** Point d'entree du canal temps reel (WebSocket + STOMP). */
  wsUrl: 'ws://localhost:8081/ws',

  /** Nom de l'application, affiche dans l'interface. */
  appName: 'Depanio',
};
