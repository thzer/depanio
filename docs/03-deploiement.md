# Mise en production

Deux chemins sont décrits ici :

| | Pour qui | Carte bancaire | Coût |
|---|---|---|---|
| **[A — Render](#chemin-a--render-gratuit-sans-carte)** | Mettre en ligne aujourd'hui, montrer le produit, démarrer | Non | 0 € |
| **[B — Google Cloud](#chemin-b--google-cloud)** | Clients payants, pas de mise en veille | Oui | 12 à 25 €/mois |

La base de données est la même dans les deux cas : **PostgreSQL chez Neon**,
gratuite. L'application fonctionne indifféremment sur PostgreSQL ou MySQL ;
seule l'URL de connexion change.

Commencez par le chemin A. Vous basculerez sur le B quand le chiffre d'affaires
le justifiera : c'est la même image, le même code, une seule commande.

---

## Chemin A — Render, gratuit, sans carte

### Ce que vous devez faire vous-même

Ces trois étapes demandent **vos comptes** ; personne ne peut les faire à votre
place :

1. un compte **GitHub** ([github.com](https://github.com)) — gratuit ;
2. un compte **Render** ([render.com](https://render.com)) — gratuit, connexion
   par GitHub, aucune carte demandée ;
3. publier ce dossier dans un dépôt GitHub (commandes ci-dessous).

### A.1 Publier le code sur GitHub

Créez un dépôt **privé** nommé `depanio` sur GitHub, puis, depuis le dossier du
projet :

```bash
git remote add origin https://github.com/VOTRE-COMPTE/depanio.git
git branch -M main
git push -u origin main
```

> Le dépôt doit rester **privé** : il porte le code que vous comptez vendre.
> Aucun mot de passe ne s'y trouve — les fichiers de secrets sont exclus — mais
> le code lui-même est votre produit.

### A.2 Créer le service sur Render

1. Sur Render : **New → Blueprint**
2. Sélectionnez le dépôt `depanio`
3. Render lit le fichier `render.yaml` et configure tout seul le service

Il vous demande ensuite les valeurs secrètes. Elles se trouvent dans votre
fichier local **`.env.production`** :

| Variable | Où la trouver |
|---|---|
| `DB_URL` | dans `.env.production` |
| `DB_USERNAME` | dans `.env.production` |
| `DB_PASSWORD` | dans `.env.production` |
| `JWT_SECRET` | dans `.env.production` |
| `HELPDESK_ADMIN_EMAIL` | votre adresse |
| `HELPDESK_ADMIN_PASSWORD` | dans `.env.production` |
| `HELPDESK_ADMIN_NAME` | votre nom |

Le premier déploiement dure une dizaine de minutes : Render compile le frontend,
puis le backend, puis démarre l'image.

### A.3 Votre application est en ligne

Render affiche une adresse du type :

```
https://depanio.onrender.com
```

Connectez-vous avec le compte administrateur. La base est vide : enregistrez
votre premier établissement, son matériel, puis les comptes de vos techniciens.

### A.4 Après le premier démarrage

Dans Render → **Environment**, passez `JPA_DDL_AUTO` de `update` à `validate` :
le schéma est en place, l'application ne doit plus pouvoir le modifier seule.

### Ce qu'il faut savoir sur l'offre gratuite

- **Mise en veille** après quinze minutes sans visite. La requête suivante
  attend environ une minute. Acceptable pour une démonstration ; à surveiller
  dès qu'un client appelle en urgence.
- **750 heures par mois**, largement suffisant pour un service.
- Chaque `git push` redéploie automatiquement.
- **Les ports du protocole SMTP sont fermés** en sortie. Voir la section
  « Courriels » ci-dessous : l'application en tient compte.

Le jour où la mise en veille devient gênante : soit l'offre payante de Render
(7 $/mois), soit le chemin B ci-dessous.

---

## Courriels de notification

Les notifications affichées dans l'application ne suffisent pas : un technicien
en déplacement n'a pas le navigateur ouvert. Le courriel est le seul canal qui
le joigne sans rien installer sur son téléphone.

### Pourquoi deux moyens d'envoi

L'application sait envoyer un courriel de deux façons, et choisit seule :

| Moyen | Quand il est retenu | Contrainte |
|---|---|---|
| **Requête HTTPS** | dès que `MAIL_API_KEY` est renseignée | dépend du fournisseur choisi |
| **SMTP** | sinon, si `MAIL_HOST` est renseigné | exige un port sortant ouvert |

Cette dualité n'est pas un luxe. Les hébergeurs mutualisés — Render, Vercel,
l'offre gratuite de la plupart des plateformes — **ferment les ports sortants
25, 465 et 587** pour ne pas devenir des relais de courriels non sollicités. Un
envoi SMTP n'y échoue pas avec un refus clair : la connexion expire au bout de
dix secondes, et seule la lecture des journaux révèle la cause.

> Cette panne a réellement été rencontrée sur ce projet. Le journal indiquait
> `MailConnectException: Couldn't connect to host, port: smtp.resend.com, 587`
> alors que la configuration SMTP était correcte. Le port 443, lui, est partout
> ouvert : d'où l'envoi par requête HTTPS, prioritaire.

Le SMTP reste en place car il est le bon choix pour une installation chez le
client ou sur un serveur loué : il n'enferme dans aucun fournisseur et accepte
aussi bien Gmail que le serveur interne d'une société.

### Configuration

Sur Render → **Environment** :

| Variable | Valeur |
|---|---|
| `MAIL_ENABLED` | `true` |
| `MAIL_FROM` | adresse d'expédition |
| `MAIL_FROM_NAME` | `Depanio` |
| `MAIL_API_KEY` | la clé fournie par votre service d'envoi |

`MAIL_API_URL` vaut par défaut `https://api.resend.com/emails` ; changez-la pour
un autre fournisseur au format de requête équivalent.

Pour un envoi en SMTP (serveur loué, Google Cloud, poste du client) :
`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, et **pas** de
`MAIL_API_KEY`.

Sans aucune de ces valeurs, l'application fonctionne normalement : les
notifications restent visibles à l'écran, elles ne partent simplement pas par
courriel.

### La limite à lever avant le premier client

Tant qu'aucun **domaine n'est vérifié** chez le service d'envoi, l'adresse
d'expédition est une adresse de test (`onboarding@resend.dev`) qui **n'atteint
que votre propre boîte**. Les courriels destinés à vos techniciens et à vos
clients sont refusés.

Pour les joindre, il faut un domaine à vous :

1. Déclarer le domaine chez le service d'envoi
2. Ajouter les enregistrements DNS qu'il indique (SPF, DKIM)
3. Attendre la vérification, puis mettre `MAIL_FROM=notifications@votre-domaine.tn`

C'est aussi ce qui évite que vos messages partent en indésirables : un courriel
envoyé depuis un domaine non authentifié est filtré par Gmail et Outlook.

### Quels événements donnent lieu à un courriel

Volontairement restreint à quatre motifs : ticket affecté, ticket déclaré,
échéance proche, délai dépassé. Un courriel pour chaque commentaire finirait en
règle de filtrage dans la boîte du technicien, et les alertes vraiment urgentes
seraient perdues avec le reste.

Un échec d'envoi est journalisé et n'a aucune autre conséquence : une messagerie
en panne ne doit jamais empêcher d'enregistrer une panne de caisse.

---

## Chemin B — Google Cloud

Ce chemin est décrit en détail dans tout ce qui suit. Comptez une heure la
première fois, puis une minute pour chaque mise à jour.

---

## B.1 Architecture retenue

```
                  ┌──────────────────────────────┐
   Navigateur ───►│  Cloud Run                   │
   (client)       │  Une seule image contenant : │
                  │   • l'API Spring Boot        │
                  │   • l'application Angular    │
                  └──────────────┬───────────────┘
                                 │ socket Unix (réseau privé)
                  ┌──────────────▼───────────────┐
                  │  Cloud SQL — MySQL 8          │
                  └──────────────────────────────┘
```

### Pourquoi une seule image plutôt que deux services

| Avantage | Conséquence concrète |
|---|---|
| Une seule adresse | Aucune configuration CORS à maintenir |
| Un seul certificat | HTTPS fourni et renouvelé automatiquement |
| WebSocket direct | Les notifications temps réel fonctionnent sans passerelle |
| Un seul déploiement | Le frontend et l'API ne peuvent pas être désynchronisés |

### Pourquoi Cloud Run

Le service **descend à zéro instance** quand personne ne l'utilise. Une société
de maintenance travaille en journée : la nuit et le week-end, vous ne payez que
la base de données. C'est le point qui change tout pour un produit qui démarre
avec peu de clients.

---

## 2. Ce que cela coûte

Estimation pour une dizaine de sociétés clientes, en région `europe-west1` :

| Poste | Formule | Coût mensuel |
|---|---|---|
| Cloud Run | 1 vCPU, 1 Gio, mise à l'échelle à zéro | 0 à 8 € |
| Cloud SQL | `db-f1-micro`, 10 Gio SSD | 9 à 12 € |
| Artifact Registry | Stockage des images | moins de 1 € |
| Secret Manager | Deux secrets | moins de 1 € |
| Sortie réseau | Usage courant | 1 à 3 € |
| **Total** | | **environ 12 à 25 € par mois** |

Google offre **300 $ de crédit** sur 90 jours à l'ouverture d'un compte : les
premiers mois ne coûtent donc rien.

> Avec trois clients à 219 DT par mois, l'hébergement représente moins de 3 %
> du chiffre d'affaires.

---

## 3. Ce que vous devez faire vous-même

Ces étapes demandent **votre compte et votre carte bancaire** ; personne ne peut
les réaliser à votre place :

1. créer un compte Google Cloud sur [console.cloud.google.com](https://console.cloud.google.com) ;
2. activer la facturation sur le projet ;
3. installer l'outil `gcloud` ([guide d'installation](https://cloud.google.com/sdk/docs/install)) ;
4. acheter un nom de domaine, si vous en voulez un.

Le reste est automatisé par le script fourni.

---

## 4. Préparation, une seule fois

### 4.1 Créer le projet

```bash
gcloud auth login

gcloud projects create depanio-prod --name="Depanio"
gcloud config set project depanio-prod
```

Activez ensuite la facturation depuis la console, section **Facturation**.

### 4.2 Activer les services utilisés

```bash
gcloud services enable \
  run.googleapis.com \
  sqladmin.googleapis.com \
  cloudbuild.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com
```

### B.4.3 Créer la base de données

```bash
gcloud sql instances create depanio-sql \
  --database-version=MYSQL_8_0 \
  --tier=db-f1-micro \
  --region=europe-west1 \
  --storage-size=10GB \
  --storage-auto-increase \
  --backup-start-time=02:00
```

> `--backup-start-time` active une sauvegarde quotidienne automatique. Ne la
> retirez pas : c'est votre seule protection contre une suppression accidentelle.

```bash
gcloud sql databases create depanio --instance=depanio-sql

# Générez un mot de passe solide et conservez-le hors du dépôt
MDP="$(openssl rand -base64 24)"
gcloud sql users create depanio --instance=depanio-sql --password="$MDP"
```

### 4.4 Enregistrer les secrets

Les valeurs sensibles ne sont jamais écrites dans un fichier du projet : elles
sont confiées à **Secret Manager**, qui les transmet à l'application au
démarrage.

```bash
# Mot de passe de la base
printf '%s' "$MDP" | gcloud secrets create depanio-db-password --data-file=-

# Clé de signature des jetons, tirée au hasard
openssl rand -base64 48 | tr -d '\n' | \
  gcloud secrets create depanio-jwt --data-file=-

# Mot de passe de votre compte administrateur.
# Choisissez-en un d'au moins 10 caractères : c'est celui de votre
# première connexion, vous pourrez le changer ensuite dans l'application.
printf '%s' 'VOTRE_MOT_DE_PASSE' | \
  gcloud secrets create depanio-admin-password --data-file=-
```

### 4.5 Autoriser le service à lire ces secrets

```bash
NUMERO="$(gcloud projects describe depanio-prod --format='value(projectNumber)')"
COMPTE="${NUMERO}-compute@developer.gserviceaccount.com"

for secret in depanio-db-password depanio-jwt depanio-admin-password; do
  gcloud secrets add-iam-policy-binding "$secret" \
    --member="serviceAccount:${COMPTE}" \
    --role="roles/secretmanager.secretAccessor"
done

gcloud projects add-iam-policy-binding depanio-prod \
  --member="serviceAccount:${COMPTE}" \
  --role="roles/cloudsql.client"
```

---

## 5. Déployer

```bash
export GCP_PROJECT=depanio-prod
export GCP_REGION=europe-west1
export ADMIN_EMAIL=vous@votre-societe.tn

./deploy/deploy.sh
```

Le script construit l'image sur Cloud Build, la publie, met le service à jour et
affiche l'adresse publique, du type :

```
https://depanio-xxxxxxxxxx-ew.a.run.app
```

**Chaque mise à jour ultérieure se résume à relancer cette même commande.**

### Premier démarrage

La variable `JPA_DDL_AUTO=update` laisse Hibernate créer les tables au premier
lancement. Une fois le schéma en place, passez-la à `validate` :

```bash
gcloud run services update depanio \
  --region europe-west1 \
  --set-env-vars JPA_DDL_AUTO=validate
```

L'application ne modifiera alors plus jamais le schéma d'elle-même.

### Le compte administrateur

La base de production démarre **entièrement vide**. L'application crée alors un
seul compte administrateur, à partir des valeurs que vous avez fournies — c'est
le rôle des variables `ADMIN_EMAIL` et du secret `depanio-admin-password`
définis plus haut.

Ce compte n'est créé **que si la table des utilisateurs est vide**. Aucun
redémarrage ultérieur ne peut écraser votre mot de passe ni recréer un compte
que vous auriez désactivé.

Une fois connecté, changez votre mot de passe depuis l'application, puis créez
vos clients, votre matériel et les comptes de vos techniciens.

> Si la base est vide et qu'aucun compte n'est configuré, l'application démarre
> normalement mais inscrit un avertissement dans ses journaux. Aucun compte par
> défaut n'est jamais créé : un mot de passe connu d'avance ouvrirait l'accès à
> toutes les données de vos clients.

---

## 6. Nom de domaine

Un `.run.app` suffit techniquement, mais `app.depanio.tn` inspire davantage
confiance à un client qui s'apprête à signer.

```bash
gcloud beta run domain-mappings create \
  --service depanio \
  --domain app.depanio.tn \
  --region europe-west1
```

La commande affiche les enregistrements DNS à créer chez votre registraire. Le
certificat HTTPS est ensuite émis et renouvelé automatiquement.

Pensez à réaligner l'adresse publique, que les étiquettes QR utilisent :

```bash
gcloud run services update depanio \
  --region europe-west1 \
  --set-env-vars FRONTEND_URL=https://app.depanio.tn
```

---

## 7. Exploitation au quotidien

### Consulter les journaux

```bash
gcloud run services logs tail depanio --region europe-west1
```

### Revenir à la version précédente

Chaque déploiement crée une révision. En cas de problème, le retour est immédiat
et ne nécessite aucune reconstruction :

```bash
gcloud run revisions list --service depanio --region europe-west1

gcloud run services update-traffic depanio \
  --to-revisions=depanio-00007-abc=100 \
  --region europe-west1
```

### Sauvegarder avant une opération sensible

```bash
gcloud sql backups create --instance=depanio-sql
```

### Surveiller la dépense

Fixez une alerte de budget dès le premier jour, dans **Facturation → Budgets** :
vous serez prévenu par courriel avant toute dérive.

---

## 8. Essayer en local avant de publier

Vérifiez l'image exactement telle qu'elle sera déployée, avec sa propre base :

```bash
docker compose up --build
```

L'application répond sur `http://localhost:8080`. Si elle fonctionne ici, elle
fonctionnera sur Cloud Run : c'est la même image.

---

## 9. Avant d'ouvrir à de vrais clients

| Point | Pourquoi |
|---|---|
| `JPA_DDL_AUTO=validate` | Empêche toute modification automatique du schéma |
| `SWAGGER_ENABLED` non défini | La documentation de l'API reste fermée au public |
| Sauvegardes vérifiées | Une sauvegarde jamais restaurée n'est pas une sauvegarde : testez-en une |
| Alerte de budget | Évite la mauvaise surprise en fin de mois |
| Mentions légales et CGU | Obligatoires dès lors que vous facturez un service |
| Registre des traitements | Vous hébergez les données de vos clients : documentez-le |

---

## 10. En cas de blocage

| Symptôme | Cause habituelle |
|---|---|
| `Cloud SQL instance not found` | Région différente entre l'instance et le service |
| `Access denied for user` | Le secret contient un retour à la ligne : recréez-le avec `printf '%s'` |
| Page blanche, API fonctionnelle | Le frontend n'a pas été compilé : vérifiez l'étape 1 du Dockerfile |
| `Permission denied on secret` | L'autorisation de l'étape 4.5 n'a pas été accordée |
| Démarrage lent après inactivité | Comportement normal de la mise à l'échelle à zéro ; `--min-instances 1` l'évite, moyennant un coût fixe |

---

Depanio — Documentation technique
