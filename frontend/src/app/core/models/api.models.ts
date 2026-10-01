import {
  EquipmentType,
  EstablishmentType,
  NotificationType,
  Priority,
  Role,
  TicketCategory,
  TicketStatus,
} from './enums';

/**
 * Objets echanges avec l'API.
 *
 * Ces interfaces reprennent exactement la forme des reponses du backend.
 * Les declarer permet au compilateur de signaler toute erreur de nom de
 * champ des la compilation, plutot qu'a l'execution dans le navigateur.
 */

// ---------------------------------------------------------------- Pagination

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

// ------------------------------------------------------------ Authentification

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  /** Duree de validite du jeton d'acces, en secondes. */
  expiresIn: number;
  user: UserResponse;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

// ------------------------------------------------------------------ Utilisateurs

export interface UserResponse {
  id: number;
  fullName: string;
  email: string;
  phone?: string;
  role: Role;
  roleLabel: string;
  speciality?: string;
  establishmentId?: number;
  establishmentName?: string;
  active: boolean;
  createdAt: string;
}

export interface CreateUserRequest {
  fullName: string;
  email: string;
  password: string;
  phone?: string;
  role: Role;
  establishmentId?: number;
  speciality?: string;
}

export interface UpdateUserRequest {
  fullName?: string;
  email?: string;
  phone?: string;
  speciality?: string;
  active?: boolean;
}

export interface TechnicianWorkload {
  technicianId: number;
  fullName: string;
  speciality?: string;
  openTickets: number;
  breachedTickets: number;
}

// --------------------------------------------------------------- Etablissements

export interface EstablishmentResponse {
  id: number;
  name: string;
  type: EstablishmentType;
  typeLabel: string;
  address?: string;
  city: string;
  phone?: string;
  contactName?: string;
  contactEmail?: string;
  contractNumber?: string;
  active: boolean;
  equipmentCount: number;
  openTicketCount: number;
  createdAt: string;
}

export interface EstablishmentRequest {
  name: string;
  type: EstablishmentType;
  address?: string;
  city: string;
  phone?: string;
  contactName?: string;
  contactEmail?: string;
  contractNumber?: string;
  active?: boolean;
}

// ---------------------------------------------------------------------- Materiel

export interface EquipmentResponse {
  id: number;
  name: string;
  type: EquipmentType;
  typeLabel: string;
  brand?: string;
  model?: string;
  serialNumber?: string;
  location?: string;
  qrToken: string;
  /** URL encodee dans l'etiquette QR. */
  qrContent: string;
  installedAt?: string;
  warrantyEnd?: string;
  underWarranty: boolean;
  active: boolean;
  establishmentId: number;
  establishmentName: string;
}

export interface EquipmentRequest {
  name: string;
  type: EquipmentType;
  brand?: string;
  model?: string;
  serialNumber?: string;
  location?: string;
  installedAt?: string;
  warrantyEnd?: string;
  establishmentId: number;
  active?: boolean;
}

export interface MostProblematicEquipment {
  equipment: EquipmentResponse;
  ticketCount: number;
}

// ----------------------------------------------------------------------- Tickets

export interface TicketSummary {
  id: number;
  reference: string;
  title: string;
  category: TicketCategory;
  categoryLabel: string;
  priority: Priority;
  priorityLabel: string;
  status: TicketStatus;
  statusLabel: string;
  establishmentName: string;
  equipmentName?: string;
  assignedToName?: string;
  createdAt: string;
  slaDueAt?: string;
  slaBreached: boolean;
  overdue: boolean;
  /** Heures restantes avant l'echeance ; negatif si elle est depassee. */
  hoursRemaining?: number;
}

export interface TicketComment {
  id: number;
  content: string;
  internalNote: boolean;
  authorId: number;
  authorName: string;
  authorRole: string;
  createdAt: string;
}

export interface TicketHistoryEntry {
  id: number;
  action: string;
  actionLabel: string;
  oldValue?: string;
  newValue?: string;
  comment?: string;
  actorName: string;
  createdAt: string;
}

export interface TicketDetail {
  id: number;
  reference: string;
  title: string;
  description: string;
  category: TicketCategory;
  categoryLabel: string;
  priority: Priority;
  priorityLabel: string;
  status: TicketStatus;
  statusLabel: string;

  establishmentId: number;
  establishmentName: string;
  establishmentCity?: string;
  establishmentPhone?: string;

  equipmentId?: number;
  equipmentName?: string;
  equipmentLocation?: string;

  createdById: number;
  createdByName: string;

  assignedToId?: number;
  assignedToName?: string;

  createdAt: string;
  updatedAt?: string;
  assignedAt?: string;
  resolvedAt?: string;
  closedAt?: string;
  slaDueAt?: string;

  slaBreached: boolean;
  overdue: boolean;
  hoursRemaining?: number;
  resolutionTimeHours?: number;

  resolutionNote?: string;
  satisfactionRating?: number;

  /** Statuts atteignables depuis l'etat courant, calcules par le serveur. */
  allowedTransitions: TicketStatus[];
  comments: TicketComment[];
  history: TicketHistoryEntry[];
}

export interface CreateTicketRequest {
  title: string;
  description: string;
  category: TicketCategory;
  priority: Priority;
  equipmentId?: number;
  establishmentId?: number;
}

export interface UpdateTicketRequest {
  title?: string;
  description?: string;
  category?: TicketCategory;
  priority?: Priority;
  equipmentId?: number;
}

export interface ChangeStatusRequest {
  status: TicketStatus;
  resolutionNote?: string;
  satisfactionRating?: number;
}

export interface TicketSearchParams {
  establishmentId?: number;
  assignedToId?: number;
  status?: TicketStatus;
  priority?: Priority;
  onlyOpen?: boolean;
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
}

// ------------------------------------------------------------------ Notifications

export interface NotificationResponse {
  id: number;
  type: NotificationType;
  typeLabel: string;
  message: string;
  read: boolean;
  ticketId?: number;
  ticketReference?: string;
  createdAt: string;
}

// --------------------------------------------------------------- Tableau de bord

export interface ChartPoint {
  label: string;
  value: number;
  key: string;
}

export interface Dashboard {
  totalTickets: number;
  openTickets: number;
  resolvedTickets: number;
  breachedTickets: number;
  ticketsThisMonth: number;
  averageResolutionHours?: number;
  /** Pourcentage de tickets traites dans le delai contractuel. */
  slaComplianceRate: number;
  byStatus: ChartPoint[];
  byPriority: ChartPoint[];
  byCategory: ChartPoint[];
  byTechnician: ChartPoint[];
  monthlyTrend: ChartPoint[];
  urgentTickets: TicketSummary[];
}

// ------------------------------------------------------------------------ Erreurs

/** Format unique des reponses d'erreur de l'API. */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
}
