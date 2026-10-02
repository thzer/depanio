# Depanio

**L'assistance technique des cafés et des restaurants.**

Depanio remplace les appels et les messages perdus par un circuit écrit : chaque
panne de caisse, de terminal de paiement ou de machine à café devient un ticket
numéroté, priorisé, affecté, mesuré et archivé.

---

## Le problème

Une société de maintenance qui entretient le matériel de plusieurs cafés et
restaurants reçoit ses demandes par téléphone et par messagerie. Quatre
faiblesses en découlent :

- **Des demandes perdues.** Un appel reçu en déplacement n'est noté nulle part.
- **Aucune priorisation.** Une caisse hors service passe après une demande de menu.
- **Aucun engagement mesurable.** Personne ne sait combien de temps a pris la réparation.
- **Aucune mémoire.** Une machine qui tombe en panne chaque mois passe inaperçue.

## Ce que Depanio apporte

| | |
|---|---|
| **Tickets** | Référence unique, priorité, responsable, échéance, historique complet |
| **Délais contractuels** | Calculés à la déclaration, alerte avant échéance, dépassement constaté automatiquement |
| **Étiquettes QR** | Un code par machine : le gérant scanne, décrit, c'est déclaré |
| **Temps réel** | Affectations, changements de statut et messages poussés instantanément |
| **Cloisonnement** | Deux clients concurrents sur la même plateforme ne se voient jamais |
| **Pilotage** | Délai moyen, taux de respect des engagements, matériel le plus défaillant |
| **Piste d'audit** | Chaque action tracée, de façon immuable |

## Rôles

| Rôle | Périmètre |
|---|---|
| **Administrateur** | Clients, matériel, comptes, affectation, statistiques globales |
| **Technicien** | Ses tickets et la file d'attente, changements de statut, notes internes |
| **Client** (gérant) | Son établissement : déclaration, suivi, échanges, confirmation |

---

## Architecture

```
┌──────────────────┐   REST + JWT / WebSocket   ┌──────────────────┐
│  Angular 20      │ ◄────────────────────────► │  Spring Boot 3.5 │
└──────────────────┘                            └────────┬─────────┘
                                                         │ JPA
                                                ┌────────▼─────────┐
                                                │  MySQL 8         │
                                                └──────────────────┘
```

En production, les deux sont livrés dans **une seule image** : une adresse, un
certificat, aucune configuration de partage entre domaines.

### Organisation du code

```
depanio/
├── backend/          API Spring Boot
│   └── src/main/java/tn/iset/gabes/helpdesk/
│       ├── controller/   Points d'entrée REST
│       ├── service/      Règles métier, contrôle d'accès, transactions
│       ├── repository/   Accès aux données
│       ├── entity/       Modèle persistant
│       ├── dto/          Objets d'échange
│       ├── security/     JWT, filtre, contexte
│       └── config/       Sécurité, WebSocket, OpenAPI, paramètres
├── frontend/         Application Angular
│   └── src/app/
│       ├── core/         Services, intercepteurs, gardes, modèles
│       ├── features/     Écrans
│       ├── layout/       Cadre de l'application
│       └── shared/       Composants réutilisables
├── deploy/           Script de déploiement
├── docs/             Cahier des charges, conception, déploiement
└── Dockerfile        Image de production
```

### Technologies

| Composant | Choix | Version |
|---|---|---|
| Langage | Java | 17 |
| Backend | Spring Boot | 3.5.6 |
| Sécurité | Spring Security + JWT | 0.12.6 |
| Persistance | Spring Data JPA / Hibernate | — |
| Temps réel | WebSocket + STOMP | — |
| QR codes | ZXing | 3.5.3 |
| Frontend | Angular (composants autonomes, signaux) | 20 |
| Graphiques | Chart.js | 4 |
| Base de données | MySQL / MariaDB | 8 / 10.4 |
| Hébergement | Google Cloud Run + Cloud SQL | — |

---

## Démarrer en développement

### Prérequis

- **JDK 17 ou supérieur** — `java -version`
- **Node.js 18 ou supérieur** — `node -v`
- **MySQL**, par exemple via XAMPP

Maven n'a pas besoin d'être installé : le projet contient son *wrapper*.

### 1. Base de données

Démarrez MySQL depuis le panneau XAMPP. La base `helpdesk_db` est créée au
premier lancement.

### 2. API

```bash
cd backend && ./mvnw spring-boot:run
```

L'API écoute sur `http://localhost:8081`.

> Le port 8081 et non 8080 : ce dernier est souvent déjà pris sur un poste de
> développement. Il se change par la variable `SERVER_PORT`.

### 3. Application

```bash
cd frontend && npm install && npm start
```

L'application s'ouvre sur `http://localhost:4200`.

### Vérifier

| Adresse | Contenu |
|---|---|
| http://localhost:4200 | L'application |
| http://localhost:8081/swagger-ui.html | Documentation interactive de l'API |
| http://localhost:8081/actuator/health | État du service |

---

## Premier démarrage

**La base démarre vide.** Aucun client, aucun matériel, aucun ticket, aucune
donnée d'exemple : vous saisissez votre activité réelle dès la première
connexion.

Un **unique compte administrateur** est créé au tout premier lancement, à partir
de votre configuration locale :

```bash
cd backend
cp secrets.properties.example secrets.properties
```

Renseignez-y votre adresse et un mot de passe d'au moins 10 caractères :

```properties
helpdesk.admin.email=vous@votre-societe.tn
helpdesk.admin.password=votre-mot-de-passe
helpdesk.admin.full-name=Votre Nom
```

Ce fichier est exclu de Git : vos identifiants ne partent jamais dans le dépôt.

> Le compte n'est créé **que si la table des utilisateurs est vide**. Une fois en
> place, modifier ce fichier n'a plus aucun effet : changez votre mot de passe
> depuis l'application. Aucun compte par défaut n'existe, car un mot de passe
> connu d'avance ouvrirait l'accès à toutes les données.

### Les premiers pas dans l'application

1. **Établissements** → enregistrez votre premier client
2. **Matériel** → ajoutez ses machines ; chacune reçoit une étiquette QR à imprimer
3. **Utilisateurs** → créez les comptes de vos techniciens et du gérant
4. Le gérant peut dès lors déclarer ses pannes, en scannant ou depuis son écran

---

## Mise en production

Deux chemins, détaillés dans **[docs/03-deploiement.md](docs/03-deploiement.md)** :

| | Pour qui | Carte bancaire | Coût |
|---|---|---|---|
| **Render** | Mettre en ligne aujourd'hui, montrer le produit | Non | 0 € |
| **Google Cloud Run** | Clients payants, pas de mise en veille | Oui | 12 à 25 €/mois |

La base de données est la même dans les deux cas : **PostgreSQL chez Neon**,
gratuite. L'application ne contient aucune requête propre à un moteur : elle
fonctionne indifféremment sur PostgreSQL ou MySQL, seule l'URL change.

Sur Render, le fichier `render.yaml` configure le service tout seul : il suffit
de connecter le dépôt et de coller les valeurs secrètes.

Sur Google Cloud, tout est automatisé :

```bash
export GCP_PROJECT=votre-projet
export ADMIN_EMAIL=vous@votre-societe.tn
./deploy/deploy.sh
```

### Essayer l'image de production en local

```bash
docker compose up --build
```

L'application répond sur `http://localhost:8080`, avec sa propre base.

---

## Tests

```bash
cd backend && ./mvnw test
```

| Suite | Vérifie |
|---|---|
| `TicketStatusTest` | Le cycle de vie : transitions, états finaux, absence de retour arrière |
| `JwtServiceTest` | Production, expiration et rejet d'un jeton forgé |
| `TenantIsolationIntegrationTest` | Le cloisonnement, par de vraies requêtes HTTP |

Les tests s'exécutent sur une base H2 en mémoire : aucune base externe requise,
et la base de développement n'est jamais touchée.

---

## Cycle de vie d'un ticket

```
NOUVEAU ──┬──► AFFECTÉ ──┬──► EN COURS ──┬──► EN ATTENTE DU CLIENT ──┐
          │              │               │                           │
          │              │               └──► RÉSOLU ◄───────────────┘
          │              │                      │  ▲
          │              │                      │  └── réouverture si
          │              │                      ▼      solution incomplète
          │              │                   CLÔTURÉ
          └──────────────┴──► ANNULÉ
```

Les transitions autorisées sont déclarées dans `TicketStatus` et vérifiées par
le serveur. Celui-ci publie dans chaque ticket la liste des actions réellement
possibles **pour l'utilisateur courant** : l'interface n'affiche donc jamais un
bouton qui serait refusé.

## Délais contractuels

| Priorité | Délai |
|---|---|
| Critique | 2 heures |
| Haute | 8 heures |
| Moyenne | 24 heures |
| Basse | 72 heures |

Paramétrables dans `application.yml`, clé `helpdesk.sla`, sans recompilation.

Une tâche planifiée s'exécute toutes les cinq minutes : elle prévient le
technicien une heure avant l'échéance et constate les dépassements. Le décompte
s'arrête à la **résolution**, l'engagement portant sur le temps de résolution.

---

## Configuration

Toutes les valeurs sensibles se surchargent par variables d'environnement.

| Variable | Rôle | Défaut en développement |
|---|---|---|
| `SERVER_PORT` | Port d'écoute | `8081` |
| `DB_USERNAME` | Utilisateur de la base | `root` |
| `DB_PASSWORD` | Mot de passe | *(vide, défaut XAMPP)* |
| `JWT_SECRET` | Clé de signature | valeur de développement |
| `HELPDESK_ADMIN_EMAIL` | Adresse du compte initial | *(aucune)* |
| `HELPDESK_ADMIN_PASSWORD` | Mot de passe du compte initial | *(aucun)* |
| `FRONTEND_URL` | Adresse publique, encodée dans les QR codes | `http://localhost:4200` |
| `SWAGGER_ENABLED` | Documentation de l'API | activée en développement |

Le profil `prod` n'accepte **aucun** défaut pour la base : l'application refuse
de démarrer si la configuration manque, plutôt que de se connecter par erreur à
une base de développement.

---

## Documentation

| Document | Contenu |
|---|---|
| [docs/01-cahier-des-charges.md](docs/01-cahier-des-charges.md) | Problématique, besoins, règles de gestion |
| [docs/02-conception.md](docs/02-conception.md) | Diagrammes de cas d'utilisation, de classes, de séquence, d'états |
| [docs/03-deploiement.md](docs/03-deploiement.md) | Mise en production : Render ou Google Cloud |
| [docs/04-securite.md](docs/04-securite.md) | Analyse de sécurité : défauts corrigés, limites assumées |

---

© Depanio
