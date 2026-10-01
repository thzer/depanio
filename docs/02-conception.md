# Dossier de conception

**Projet :** Système de gestion des tickets d'assistance — ISET Gabès

Les diagrammes de ce document sont écrits en **Mermaid**. Ils s'affichent
directement sur GitHub et dans VS Code (extension *Markdown Preview Mermaid
Support*), et peuvent être exportés en image pour le rapport depuis
[mermaid.live](https://mermaid.live).

---

## 1. Architecture générale

```mermaid
graph LR
    subgraph Navigateur
        A[Application Angular]
    end

    subgraph Serveur
        B[API REST<br/>Spring Boot]
        C[(MySQL / MariaDB)]
    end

    A -->|HTTPS + JWT| B
    A <-->|WebSocket STOMP| B
    B -->|JPA / Hibernate| C
```

L'application suit une architecture **client-serveur découplée** : le frontend et
le backend sont deux applications distinctes qui ne communiquent que par une API
REST. Ce découplage permet d'ajouter plus tard une application mobile consommant
la même API, sans rien modifier côté serveur.

### Couches du backend

```mermaid
graph TD
    C[controller<br/>Points d entrée REST] --> S[service<br/>Règles métier et contrôle d accès]
    S --> R[repository<br/>Accès aux données]
    R --> E[entity<br/>Modèle persistant]
    C -.-> D[dto<br/>Objets d échange]
    S -.-> M[mapper<br/>Conversion entité / DTO]
```

**Principe directeur :** aucune règle métier dans les contrôleurs. Un contrôleur
valide le format de la requête et délègue. Toutes les décisions — qui a le droit
de voir quoi, quelle transition est permise — sont prises dans la couche service.
Un nouveau point d'entrée ne peut donc pas contourner une règle par oubli.

---

## 2. Diagramme de cas d'utilisation

```mermaid
graph TB
    Client([Client<br/>gérant])
    Tech([Technicien])
    Admin([Administrateur])

    UC1[Déclarer une panne]
    UC2[Scanner une étiquette QR]
    UC3[Suivre ses tickets]
    UC4[Échanger des messages]
    UC5[Confirmer la clôture]

    UC6[Consulter les tickets affectés]
    UC7[Prendre en charge un ticket]
    UC8[Faire évoluer le statut]
    UC9[Documenter la résolution]

    UC10[Gérer les établissements]
    UC11[Gérer le matériel]
    UC12[Gérer les comptes]
    UC13[Affecter un ticket]
    UC14[Consulter les indicateurs]

    Client --> UC1
    Client --> UC2
    Client --> UC3
    Client --> UC4
    Client --> UC5

    Tech --> UC6
    Tech --> UC7
    Tech --> UC8
    Tech --> UC9
    Tech --> UC4

    Admin --> UC10
    Admin --> UC11
    Admin --> UC12
    Admin --> UC13
    Admin --> UC14

    UC2 -.->|include| UC1
    UC9 -.->|include| UC8
```

---

## 3. Diagramme de classes

```mermaid
classDiagram
    class BaseEntity {
        <<abstract>>
        +Long id
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }

    class Establishment {
        +String name
        +EstablishmentType type
        +String address
        +String city
        +String phone
        +String contractNumber
        +boolean active
    }

    class User {
        +String fullName
        +String email
        +String password
        +Role role
        +String speciality
        +boolean active
        +isTenantScoped() boolean
    }

    class Equipment {
        +String name
        +EquipmentType type
        +String serialNumber
        +String location
        +String qrToken
        +LocalDate warrantyEnd
        +isUnderWarranty() boolean
    }

    class Ticket {
        +String reference
        +String title
        +String description
        +TicketCategory category
        +Priority priority
        +TicketStatus status
        +LocalDateTime slaDueAt
        +boolean slaBreached
        +String resolutionNote
        +Integer satisfactionRating
        +isOverdue() boolean
        +getTimeRemaining() Duration
        +getResolutionTime() Duration
    }

    class TicketComment {
        +String content
        +boolean internalNote
    }

    class TicketHistory {
        +HistoryAction action
        +String oldValue
        +String newValue
    }

    class Notification {
        +NotificationType type
        +String message
        +boolean read
        +markAsRead()
    }

    BaseEntity <|-- Establishment
    BaseEntity <|-- User
    BaseEntity <|-- Equipment
    BaseEntity <|-- Ticket
    BaseEntity <|-- TicketComment
    BaseEntity <|-- TicketHistory
    BaseEntity <|-- Notification

    Establishment "1" o-- "*" Equipment : possède
    Establishment "1" o-- "*" User : emploie
    Establishment "1" --> "*" Ticket : concerne
    Equipment "0..1" --> "*" Ticket : fait l objet de
    User "1" --> "*" Ticket : déclare
    User "0..1" --> "*" Ticket : traite
    Ticket "1" *-- "*" TicketComment : contient
    Ticket "1" *-- "*" TicketHistory : trace
    User "1" --> "*" Notification : reçoit
```

### Choix de conception commentés

| Choix | Justification |
|---|---|
| `BaseEntity` abstraite | Les dates de création et de modification concernent toutes les entités ; les centraliser évite de les répéter et de les oublier |
| `Establishment` porte le cloisonnement | Une seule relation à suivre pour savoir si un utilisateur a le droit de voir une donnée |
| `qrToken` distinct de l'identifiant | L'étiquette collée sur une machine ne doit pas révéler la numérotation interne de la base |
| `TicketHistory` séparée des commentaires | L'historique est une piste d'audit produite par le système ; les commentaires sont une conversation entre personnes |
| `internalNote` sur le commentaire | Permet une seule table d'échanges au lieu de deux, le filtrage se faisant à la lecture |
| Transitions portées par l'énumération | La règle vit à côté de la donnée qu'elle contraint, et le serveur peut les publier au frontend |

---

## 4. Modèle physique des données

```mermaid
erDiagram
    ESTABLISHMENTS ||--o{ EQUIPMENTS : possede
    ESTABLISHMENTS ||--o{ USERS : emploie
    ESTABLISHMENTS ||--o{ TICKETS : concerne
    EQUIPMENTS ||--o{ TICKETS : subit
    USERS ||--o{ TICKETS : declare
    USERS ||--o{ TICKET_COMMENTS : redige
    TICKETS ||--o{ TICKET_COMMENTS : contient
    TICKETS ||--o{ TICKET_HISTORY : trace
    TICKETS ||--o{ NOTIFICATIONS : declenche
    USERS ||--o{ NOTIFICATIONS : recoit

    ESTABLISHMENTS {
        bigint id PK
        varchar name
        varchar type
        varchar city
        varchar contract_number
        boolean active
    }

    USERS {
        bigint id PK
        varchar full_name
        varchar email UK
        varchar password
        varchar role
        bigint establishment_id FK
        boolean active
    }

    EQUIPMENTS {
        bigint id PK
        varchar name
        varchar type
        varchar serial_number
        varchar qr_token UK
        date warranty_end
        bigint establishment_id FK
    }

    TICKETS {
        bigint id PK
        varchar reference UK
        varchar title
        text description
        varchar category
        varchar priority
        varchar status
        datetime sla_due_at
        boolean sla_breached
        datetime resolved_at
        bigint establishment_id FK
        bigint equipment_id FK
        bigint created_by_id FK
        bigint assigned_to_id FK
    }

    TICKET_COMMENTS {
        bigint id PK
        text content
        boolean internal_note
        bigint ticket_id FK
        bigint author_id FK
    }

    TICKET_HISTORY {
        bigint id PK
        varchar action
        varchar old_value
        varchar new_value
        bigint ticket_id FK
        bigint actor_id FK
    }
```

### Index

Les colonnes les plus souvent filtrées portent un index, afin que les listes et le
tableau de bord restent rapides lorsque le volume augmente :

| Table | Index | Requête servie |
|---|---|---|
| `tickets` | `status` | Liste filtrée par statut |
| `tickets` | `priority` | Liste filtrée par urgence |
| `tickets` | `establishment_id` | Cloisonnement par client |
| `tickets` | `assigned_to_id` | Tickets d'un technicien |
| `tickets` | `sla_due_at` | Surveillance des échéances |
| `notifications` | `recipient_id, is_read` | Compteur de notifications non lues |

---

## 5. Diagramme de séquence : déclaration et résolution d'une panne

```mermaid
sequenceDiagram
    actor G as Gérant
    participant F as Frontend
    participant A as API
    participant D as Base
    actor T as Technicien

    G->>F: Scanne l étiquette QR
    F->>A: GET /api/equipments/by-qr/{token}
    A->>D: Recherche du matériel
    A-->>F: Fiche du matériel
    F-->>G: Formulaire pré-rempli

    G->>F: Décrit la panne, urgence Haute
    F->>A: POST /api/tickets/from-qr/{token}
    A->>A: Génère TK-2026-0042
    A->>A: Calcule l échéance (+8 h)
    A->>D: Enregistre le ticket et son historique
    A-->>F: Ticket créé
    A-)T: Notification WebSocket

    T->>F: Prend le ticket en charge
    F->>A: PATCH /api/tickets/42/assign
    A->>A: Vérifie le rôle et le statut
    A->>D: Affecte et horodate
    A-)G: Notification WebSocket

    T->>F: Déclare le ticket résolu
    F->>A: PATCH /api/tickets/42/status
    A->>A: Vérifie la transition autorisée
    A->>A: Exige la note de résolution
    A->>D: Enregistre, arrête le décompte SLA
    A-)G: Notification WebSocket

    G->>F: Confirme, note 5/5
    F->>A: PATCH /api/tickets/42/status (CLOSED)
    A->>D: Clôture et enregistre la satisfaction
```

---

## 6. Diagramme d'états du ticket

```mermaid
stateDiagram-v2
    [*] --> NOUVEAU : déclaration

    NOUVEAU --> AFFECTE : affectation
    NOUVEAU --> EN_COURS : prise en charge directe
    NOUVEAU --> ANNULE : abandon

    AFFECTE --> EN_COURS : début du traitement
    AFFECTE --> EN_ATTENTE : information manquante
    AFFECTE --> ANNULE

    EN_COURS --> EN_ATTENTE : attente du client
    EN_COURS --> RESOLU : solution appliquée
    EN_COURS --> ANNULE

    EN_ATTENTE --> EN_COURS : reprise
    EN_ATTENTE --> RESOLU
    EN_ATTENTE --> ANNULE

    RESOLU --> CLOTURE : confirmation du client
    RESOLU --> EN_COURS : solution jugée incomplète

    CLOTURE --> [*]
    ANNULE --> [*]
```

Ces transitions sont déclarées dans l'énumération `TicketStatus`, méthode
`allowedTransitions()`. Le serveur les publie au frontend dans le champ
`allowedTransitions` de chaque ticket : l'interface n'affiche donc que les actions
réellement possibles, sans dupliquer la règle.

---

## 7. Sécurité

### 7.1 Chaîne d'authentification

```mermaid
sequenceDiagram
    participant C as Client
    participant F as JwtAuthenticationFilter
    participant S as SecurityContext
    participant Ct as Contrôleur
    participant Sv as Service

    C->>F: Requête + en-tête Authorization
    F->>F: Vérifie signature et expiration
    F->>S: Place l utilisateur authentifié
    S->>Ct: Contrôle du rôle (@PreAuthorize)
    Ct->>Sv: Appel métier
    Sv->>Sv: Applique le cloisonnement par établissement
    Sv-->>C: Données du seul périmètre autorisé
```

### 7.2 Mesures retenues

| Risque | Mesure |
|---|---|
| Vol de mot de passe en base | Empreinte BCrypt avec sel différent par compte |
| Falsification de jeton | Signature HMAC vérifiée à chaque requête |
| Accès aux données d'un autre client | Filtre imposé dans la couche service à partir du compte, jamais depuis la requête |
| Découverte de l'existence d'une ressource | Réponse 404 au lieu de 403 sur une ressource hors périmètre |
| Escalade par un paramètre d'URL | Le paramètre `establishmentId` est ignoré pour un compte client |
| Injection SQL | Requêtes paramétrées exclusivement (JPA) |
| Fuite d'informations techniques | Message générique au client, pile d'appels uniquement dans les journaux |
| Secrets dans le dépôt | Valeurs sensibles surchargeables par variables d'environnement |

---

## 8. Tests automatisés

| Suite | Portée | Vérifie |
|---|---|---|
| `TicketStatusTest` | Unitaire | Le cycle de vie : transitions autorisées, états finaux, absence de retour arrière |
| `JwtServiceTest` | Unitaire | Production, expiration et rejet d'un jeton signé avec une autre clé |
| `TenantIsolationIntegrationTest` | Intégration | Le cloisonnement, par de vraies requêtes HTTP sur une base H2 en mémoire |

Les tests d'intégration s'exécutent sur une base en mémoire : ils ne touchent pas
la base de développement et ne demandent aucun serveur MySQL démarré.

```bash
cd backend && ./mvnw test
```

---

ISET Gabès — Projet de Fin d'Études
