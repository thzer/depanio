# Helpdesk — Système de gestion des tickets d'assistance

**Application web de gestion des demandes d'intervention technique pour les cafés et les restaurants.**

Projet de Fin d'Études — ISET Gabès

---

## 1. Problématique

Une société de maintenance qui entretient le matériel de plusieurs cafés et restaurants
(caisses, terminaux de paiement, machines à café, matériel frigorifique) reçoit ses
demandes d'intervention par téléphone et par messagerie instantanée. Cette pratique
entraîne trois difficultés :

- **Des demandes perdues.** Un appel reçu pendant un déplacement n'est noté nulle part.
- **Aucune priorisation.** Une caisse hors service en pleine affluence est traitée dans
  le même ordre qu'une faute de frappe dans un menu.
- **Aucune traçabilité.** Impossible de dire combien de temps une panne a mis à être
  résolue, ni quel matériel tombe en panne de façon répétée.

Cette application remplace ce fonctionnement par un circuit écrit, mesuré et traçable.

## 2. Fonctionnalités

| Domaine | Description |
|---|---|
| **Gestion des tickets** | Déclaration, affectation, suivi d'un cycle de vie contrôlé, échanges, résolution et clôture avec note de satisfaction |
| **Cloisonnement multi-locataire** | Chaque établissement client ne voit que ses propres tickets, son propre matériel et sa propre fiche |
| **Délais contractuels (SLA)** | Échéance calculée automatiquement selon la priorité, surveillance périodique, alerte avant échéance et constat de dépassement |
| **Étiquettes QR** | Un QR code par machine : le gérant le scanne et le formulaire s'ouvre déjà rempli |
| **Notifications temps réel** | Diffusion immédiate par WebSocket, conservation en base pour consultation ultérieure |
| **Tableau de bord** | Indicateurs, répartitions par statut, priorité et catégorie, courbe mensuelle, charge par technicien |
| **Piste d'audit** | Historique immuable de chaque action réalisée sur un ticket |
| **Parc matériel** | Inventaire par établissement, suivi de garantie, classement des matériels les plus défaillants |

## 3. Rôles

| Rôle | Périmètre |
|---|---|
| **Administrateur** | Accès complet : clients, matériel, comptes, affectation des tickets, statistiques globales |
| **Technicien** | Tickets qui lui sont affectés et file d'attente, changements de statut, notes internes |
| **Client** (gérant) | Son établissement uniquement : déclaration, suivi, échanges, confirmation de clôture |

## 4. Architecture technique

```
┌─────────────────────┐      REST / JSON + JWT       ┌──────────────────────┐
│   Frontend Angular  │ ───────────────────────────► │   API Spring Boot    │
│   (navigateur)      │ ◄─────────────────────────── │   (serveur)          │
└─────────────────────┘      WebSocket / STOMP       └──────────┬───────────┘
                                                                │ JPA / Hibernate
                                                     ┌──────────▼───────────┐
                                                     │   MySQL / MariaDB    │
                                                     └──────────────────────┘
```

### Couches du backend

```
controller/   Points d'entrée REST, validation des requêtes
service/      Règles métier, contrôle d'accès, transactions
repository/   Accès aux données (Spring Data JPA)
entity/       Modèle persistant
dto/          Objets d'échange avec le frontend
mapper/       Conversion entité ↔ DTO
security/     JWT, filtre d'authentification, utilitaires de contexte
config/       Sécurité, WebSocket, OpenAPI, paramètres métier
common/       Exceptions et gestionnaire global d'erreurs
```

### Technologies

| Composant | Technologie | Version |
|---|---|---|
| Langage | Java | 17 |
| Framework backend | Spring Boot | 3.5.6 |
| Persistance | Spring Data JPA / Hibernate | — |
| Sécurité | Spring Security + JWT (jjwt) | 0.12.6 |
| Temps réel | WebSocket + STOMP | — |
| QR codes | ZXing | 3.5.3 |
| Documentation API | SpringDoc OpenAPI (Swagger UI) | 2.8.9 |
| Base de données | MariaDB (XAMPP) / MySQL | 10.4 |
| Build | Maven (via wrapper) | 3.9.16 |
| Frontend | Angular | à venir |

## 5. Démarrage

### Prérequis

- **JDK 17** ou supérieur — `java -version`
- **XAMPP** (ou MySQL) pour la base de données
- **Node.js 18+** pour le frontend

Maven n'a pas besoin d'être installé : le projet contient son *wrapper* (`mvnw`).

### Lancer le backend

1. Démarrer **MySQL** depuis le panneau de contrôle XAMPP.
   La base `helpdesk_db` est créée automatiquement au premier démarrage.

2. Dans le dossier `backend` :

```bash
./mvnw spring-boot:run
```

L'API écoute sur `http://localhost:8081`.

### Vérifier que tout fonctionne

| Adresse | Contenu |
|---|---|
| http://localhost:8081/swagger-ui.html | Documentation interactive de l'API |
| http://localhost:8081/actuator/health | État du service |

## 6. Comptes de démonstration

Au premier démarrage sous le profil `dev`, si la base est vide, un jeu de données
complet est créé : 4 établissements, 10 matériels et une quinzaine de tickets à
différents stades.

**Mot de passe commun à tous ces comptes : `Passer@123`**

| Rôle | Adresse | Observations |
|---|---|---|
| Administrateur | `admin@helpdesk.tn` | Accès complet |
| Technicien | `ahmed.tech@helpdesk.tn` | Caisses et terminaux de paiement |
| Technicien | `salma.tech@helpdesk.tn` | Froid et machines à café |
| Technicien | `karim.tech@helpdesk.tn` | Réseau et informatique |
| Client | `hichem@elmedina.tn` | Café El Medina |
| Client | `nadia@darzaman.tn` | Restaurant Dar Zaman |
| Client | `olfa@yasmine.tn` | Salon de Thé Yasmine |
| Client | `sami@oasisfood.tn` | Oasis Fast Food |

> Ces comptes sont destinés au développement et à la soutenance.
> Ils ne sont **jamais** créés sous le profil `prod`.

## 7. Essayer l'API sans frontend

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@helpdesk.tn","password":"Passer@123"}'
```

Copier la valeur de `accessToken`, puis :

```bash
curl http://localhost:8081/api/tickets?onlyOpen=true \
  -H "Authorization: Bearer COLLER_LE_JETON_ICI"
```

Depuis Swagger UI, le bouton **Authorize** évite de recopier le jeton à chaque appel.

## 8. Cycle de vie d'un ticket

```
                  ┌──────────┐
                  │ NOUVEAU  │
                  └────┬─────┘
            ┌──────────┼──────────────┐
            ▼          ▼              ▼
     ┌──────────┐ ┌──────────┐  ┌──────────┐
     │ AFFECTÉ  │ │ EN COURS │  │  ANNULÉ  │
     └────┬─────┘ └────┬─────┘  └──────────┘
          │            │
          ▼            ▼
   ┌───────────────────────┐
   │ EN ATTENTE DU CLIENT  │
   └──────────┬────────────┘
              ▼
        ┌──────────┐      ┌──────────┐
        │  RÉSOLU  │ ───► │ CLÔTURÉ  │
        └────┬─────┘      └──────────┘
             │
             └──► retour EN COURS si la résolution est jugée incomplète
```

Les transitions autorisées sont déclarées dans l'énumération `TicketStatus` et
vérifiées côté serveur : aucune requête ne peut placer un ticket dans un état
incohérent. La fiche d'un ticket expose le champ `allowedTransitions`, ce qui permet
au frontend de n'afficher que les actions réellement possibles.

## 9. Délais contractuels (SLA)

| Priorité | Délai de résolution |
|---|---|
| Critique | 2 heures |
| Haute | 8 heures |
| Moyenne | 24 heures |
| Basse | 72 heures |

Ces durées sont configurables dans `application.yml` (clé `helpdesk.sla`) : les ajuster
ne demande aucune recompilation.

Une tâche planifiée s'exécute toutes les cinq minutes, prévient le technicien une heure
avant l'échéance et marque les tickets dépassés en informant les administrateurs.

## 10. Configuration

Toutes les valeurs sensibles se surchargent par variables d'environnement, sans
modifier le code :

| Variable | Rôle | Défaut (profil dev) |
|---|---|---|
| `DB_USERNAME` | Utilisateur de la base | `root` |
| `DB_PASSWORD` | Mot de passe de la base | *(vide, défaut XAMPP)* |
| `JWT_SECRET` | Clé de signature des jetons | valeur de développement |

Le profil `prod` (`application-prod.yml`) n'accepte **aucune** valeur par défaut pour la
base de données : l'application refuse de démarrer si la configuration est absente,
plutôt que de se connecter silencieusement à une base de développement.

## 11. Structure du dépôt

```
ticketing/
├── backend/            API Spring Boot
│   ├── src/main/java/  Code source
│   ├── src/main/resources/
│   │   ├── application.yml        Configuration commune
│   │   ├── application-dev.yml    Profil développement
│   │   └── application-prod.yml   Profil production
│   └── src/test/       Tests automatisés
├── frontend/           Application Angular
├── docs/               Cahier des charges, conception, diagrammes
└── README.md
```

---

Projet de Fin d'Études — ISET Gabès
