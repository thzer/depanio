# Analyse de sécurité

**Produit :** Depanio — assistance technique pour cafés et restaurants
**Portée :** application déployée sur `https://depanio.onrender.com`
**Date de l'analyse :** 2 octobre 2026

Ce document décrit ce qui protège l'application, ce qui a été corrigé, et ce
qui reste à surveiller. Il est écrit pour être lu par un acheteur ou un jury :
rien n'y est affirmé sans avoir été vérifié sur l'application en service.

---

## 1. Ce que l'application doit protéger

| Donnée | Pourquoi elle compte |
|---|---|
| Tickets et historiques | Révèlent l'activité et les pannes de chaque établissement |
| Coordonnées des clients | Nom du gérant, téléphone, adresse |
| Parc matériel | Marques, modèles, numéros de série |
| Comptes et mots de passe | Donnent accès à tout le reste |
| Demandes de démonstration | Prospects, donc données commerciales |

Le risque central n'est pas la destruction : c'est qu'**un client voie les
données d'un autre**. Deux cafés concurrents peuvent utiliser la même
plateforme.

---

## 2. Défauts trouvés et corrigés

### 2.1 Essais de mot de passe en rafale — *critique*

**Constaté.** Douze tentatives consécutives avec un mauvais mot de passe
recevaient la même réponse, sans aucun ralentissement. Rien n'empêchait un
programme d'en essayer des milliers. Aucun mot de passe choisi par un humain ne
résiste à cela.

**Corrigé.** Les échecs sont comptés sur deux clés simultanément :

- l'**adresse visée**, pour protéger un compte même si les tentatives viennent
  de partout ;
- l'**origine réseau**, pour arrêter un balayage de plusieurs comptes depuis un
  même point.

Au-delà de six échecs, les tentatives sont suspendues quinze minutes. Une
connexion réussie efface le compteur. Un compte **désactivé** n'entre pas dans
ce décompte : ce n'est pas une erreur d'identifiants, et bloquer son titulaire
n'aurait aucun sens.

**Vérifié en service :** les six premiers essais répondent « identifiants
incorrects », le septième « Trop de tentatives. Réessayez dans 15 minutes. »

> **Limite assumée.** Le compteur vit en mémoire. Avec plusieurs instances du
> service, chacune aurait le sien et le nombre d'essais toléré serait multiplié
> d'autant. Pour une instance, c'est suffisant ; au-delà, ce compteur devra être
> partagé.

### 2.2 En-têtes de sécurité absents — *moyen*

**Constaté.** La politique de sécurité du contenu, la politique de référent et
celle des permissions n'étaient pas envoyées.

**Corrigé.** Les en-têtes suivants sont désormais présents :

| En-tête | Effet |
|---|---|
| `Content-Security-Policy` | Les scripts ne peuvent venir que de l'application |
| `Strict-Transport-Security` | HTTPS imposé pour un an |
| `X-Frame-Options: DENY` | Le site ne peut pas être affiché dans un cadre |
| `X-Content-Type-Options: nosniff` | Pas d'interprétation libre des types |
| `Referrer-Policy` | L'adresse consultée ne fuit pas vers les sites tiers |
| `Permissions-Policy` | Caméra, micro et position retirés |

`'unsafe-inline'` n'est toléré que pour les **styles** — Angular injecte ses
feuilles dans la page. Les **scripts**, eux, ne peuvent provenir que de
l'application : c'est la restriction qui compte réellement.

### 2.3 Injection dans les courriels — *moyen*

**Constaté.** Les titres de tickets et noms d'établissements, saisis par des
utilisateurs, étaient insérés tels quels dans des courriels en HTML. Il
devenait possible d'y glisser des balises, et donc de fabriquer un faux message
à l'intérieur d'un courriel paraissant venir de l'application — une invitation
à cliquer sur un lien frauduleux, par exemple.

**Corrigé.** Toute valeur venant d'un utilisateur est échappée avant insertion.

### 2.4 Destination après connexion non contrôlée — *faible*

**Constaté.** L'adresse de retour était lue dans l'URL et suivie sans
vérification. Un lien forgé pouvait déposer l'utilisateur, après une connexion
réussie, sur une page tierce lui réclamant de nouveau ses identifiants.

**Corrigé.** Seuls les chemins internes sont acceptés. `//exemple.com` est
également écarté : c'est une adresse absolue malgré sa barre oblique initiale.

### 2.5 Toute adresse inconnue répondait 500 — *faible*

**Constaté.** Une ressource absente remontait jusqu'au filet de sécurité et
était signalée comme panne interne.

**Corrigé.** Réponse 404. Au-delà du confort, cela évite que les journaux se
remplissent de fausses erreurs et finissent par masquer les vrais incidents.

### 2.6 Mot de passe minimum porté à dix caractères — *faible*

Le minimum était de huit caractères pour les comptes, dix pour l'administrateur
initial. L'exigence est unifiée à **dix**, partout.

---

## 3. Ce qui protégeait déjà, et qui a été vérifié

### 3.1 Cloisonnement entre clients

C'est la garantie la plus sensible du produit.

- Le filtre est appliqué dans la **couche service**, à partir du compte, et non
  depuis un paramètre de la requête. Un client qui force `?establishmentId=1`
  obtient malgré tout ses seules données.
- Demander le ticket d'un autre client renvoie **404 et non 403** : un 403
  confirmerait au demandeur que ce ticket existe, ce qui est déjà une
  information.
- Huit tests d'intégration attaquent l'API par de vraies requêtes HTTP pour
  vérifier ce cloisonnement.

### 3.2 Authentification

| Point | État |
|---|---|
| Mots de passe | Empreinte BCrypt, sel différent par compte, jamais stockés en clair |
| Jetons | Signature HMAC vérifiée à chaque requête |
| Jeton forgé | Rejeté — couvert par un test dédié |
| Jeton de renouvellement | Ne donne aucun accès aux ressources |
| Compte désactivé | Refusé même si son jeton est encore valide |
| Canal temps réel | Authentifié à l'ouverture, par le même jeton |

### 3.3 Autorisations

Le rôle est contrôlé sur les points d'entrée, et les règles fines dans la
couche service. Le serveur publie dans chaque ticket la liste des actions
**réellement possibles pour l'utilisateur courant** : l'interface n'affiche donc
jamais un bouton qui serait refusé, et la règle reste décidée côté serveur.

### 3.4 Injections

- **SQL** : requêtes paramétrées exclusivement, via JPA. Aucune concaténation.
- **HTML** : Angular échappe par défaut. Le seul contournement du mécanisme se
  trouve dans le composant d'icônes, et son contenu provient d'une table figée
  dans le code, jamais d'une saisie.

### 3.5 Secrets

- Aucun mot de passe ni clé dans le dépôt — vérifié sur l'ensemble de
  l'historique Git, pas seulement sur l'état actuel.
- En production, les valeurs sensibles viennent de l'hébergeur.
- Le profil de production n'accepte **aucune valeur par défaut** pour la base :
  l'application refuse de démarrer plutôt que de se connecter par erreur à une
  base de développement.

### 3.6 Surface exposée

Vérifié sur l'application en service :

| Adresse | Réponse |
|---|---|
| `/swagger-ui.html` | 404 — documentation fermée |
| `/v3/api-docs` | 404 |
| `/actuator/env`, `/metrics`, `/beans` | 401 |
| `/actuator/health` | 200 — nécessaire à l'hébergeur |
| `/api/**` sans jeton | 401 |

Seuls quatre points d'entrée sont ouverts sans authentification : la connexion,
le renouvellement de jeton, l'état de santé, et le dépôt d'une demande de
démonstration.

### 3.7 Formulaire public

Le dépôt de demande est le seul point où un inconnu écrit en base. Il est
protégé sans gêner le visiteur :

- un **champ piège**, invisible, que seuls les robots remplissent ;
- une limite de trois demandes par adresse et par heure ;
- une limite globale de quarante demandes par heure.

Le champ piège ne renvoie aucune erreur : signaler le piège apprendrait à le
contourner.

### 3.8 Base de données

- Hébergée chez Neon, accès par mot de passe sur canal chiffré.
- Aucun port de base ouvert sur l'application.
- Le schéma est en mode **`validate`** : l'application ne peut plus le modifier
  d'elle-même. Ce verrou a déjà servi — il a arrêté un déploiement qui aurait
  modifié le schéma sans que personne ne le décide.

---

## 4. Limites connues

Ces points ne sont pas des défauts cachés : ce sont des choix, assumés au stade
actuel du produit.

| Limite | Conséquence | Quand la traiter |
|---|---|---|
| **Pas de révocation de jeton** | Un jeton volé reste valable jusqu'à son expiration (12 h). La déconnexion n'efface le jeton que dans le navigateur. | Dès qu'un client manipule des données réellement sensibles |
| **Comptage des échecs en mémoire** | Multiplié par le nombre d'instances | À la première mise à l'échelle |
| **Pas de double authentification** | Un mot de passe volé suffit | Pour les comptes administrateurs, à terme |
| **Pas de journal d'audit des connexions** | On sait qui a agi sur un ticket, pas qui s'est connecté | Avant un audit client |
| **Sauvegardes non testées** | Une sauvegarde jamais restaurée n'est pas une sauvegarde | **Avant le premier client payant** |
| **Pas de politique de rétention** | Les données s'accumulent sans limite | À la signature du premier contrat |

---

## 5. À faire avant de facturer un client

1. **Restaurer une sauvegarde pour de vrai**, au moins une fois. C'est le seul
   moyen de savoir qu'elle fonctionne.
2. **Mentions légales et conditions générales** — obligatoires dès qu'un service
   est facturé.
3. **Registre des traitements** — vous hébergez les données de vos clients, il
   faut pouvoir dire lesquelles et pourquoi.
4. **Alerte de budget** chez l'hébergeur, pour éviter la dérive silencieuse.
5. **Changer le mot de passe administrateur** depuis l'application, pour qu'il
   ne soit plus celui généré à l'installation.

---

## 6. Comment ces conclusions ont été obtenues

| Méthode | Ce qu'elle a donné |
|---|---|
| Lecture du code | Injection dans les courriels, redirection non contrôlée |
| Requêtes sur l'application en service | Absence de limitation des tentatives, en-têtes manquants |
| Tests automatisés | 44 tests, dont 8 sur le cloisonnement et 6 sur la limitation |
| Vérification après correction | Chaque correctif retesté sur l'application en ligne |

Les corrections décrites ici ont toutes été **vérifiées après déploiement**, et
non seulement en local.

---

Depanio — document technique
