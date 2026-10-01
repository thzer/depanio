/**
 * Enumerations partagees avec le backend.
 *
 * Seules les valeurs techniques sont reprises ici. Les libelles affiches
 * proviennent de l'API (point d'entree /api/reference) : ils restent ainsi
 * definis a un seul endroit et ne risquent pas de diverger entre le
 * serveur et le navigateur.
 */

export type Role = 'ADMIN' | 'TECHNICIAN' | 'CLIENT';

export type TicketStatus =
  | 'NEW'
  | 'ASSIGNED'
  | 'IN_PROGRESS'
  | 'WAITING_CLIENT'
  | 'RESOLVED'
  | 'CLOSED'
  | 'CANCELLED';

export type Priority = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';

export type TicketCategory =
  | 'HARDWARE_FAILURE'
  | 'SOFTWARE_ISSUE'
  | 'NETWORK'
  | 'CONFIGURATION'
  | 'CONSUMABLE'
  | 'TRAINING'
  | 'OTHER';

export type EquipmentType =
  | 'POS_TERMINAL'
  | 'CARD_READER'
  | 'COFFEE_MACHINE'
  | 'PRINTER'
  | 'REFRIGERATION'
  | 'NETWORK_DEVICE'
  | 'SCALE'
  | 'TABLET'
  | 'OTHER';

export type EstablishmentType =
  | 'CAFE'
  | 'RESTAURANT'
  | 'TEA_HOUSE'
  | 'FAST_FOOD'
  | 'PASTRY'
  | 'OTHER';

export type NotificationType =
  | 'TICKET_CREATED'
  | 'TICKET_ASSIGNED'
  | 'TICKET_UPDATED'
  | 'TICKET_RESOLVED'
  | 'TICKET_CLOSED'
  | 'NEW_COMMENT'
  | 'SLA_WARNING'
  | 'SLA_BREACHED';

/** Couple valeur et libelle, tel que renvoye par /api/reference. */
export interface EnumOption {
  value: string;
  label: string;
}

/**
 * Classe CSS associee a chaque statut, utilisee par les pastilles de
 * couleur. Regrouper ces correspondances ici evite de les repeter dans
 * chaque gabarit.
 */
export const STATUS_CLASS: Record<TicketStatus, string> = {
  NEW: 'status-new',
  ASSIGNED: 'status-assigned',
  IN_PROGRESS: 'status-progress',
  WAITING_CLIENT: 'status-waiting',
  RESOLVED: 'status-resolved',
  CLOSED: 'status-closed',
  CANCELLED: 'status-cancelled',
};

export const PRIORITY_CLASS: Record<Priority, string> = {
  CRITICAL: 'priority-critical',
  HIGH: 'priority-high',
  MEDIUM: 'priority-medium',
  LOW: 'priority-low',
};
