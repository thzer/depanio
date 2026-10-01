/**
 * Configuration de production.
 *
 * Les adresses sont relatives : l'API est servie par le meme hote que le
 * frontend, ce qui evite toute configuration de domaine au deploiement.
 */
export const environment = {
  production: true,
  apiUrl: '/api',
  wsUrl: `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`,
  appName: 'Depanio',
};
