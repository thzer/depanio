# Mise en production sur Google Cloud

Ce guide décrit le déploiement de **Depanio** sur Google Cloud, de la création
du projet jusqu'au nom de domaine. Comptez une heure la première fois, puis une
minute pour chaque mise à jour.

---

## 1. Architecture retenue

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

### 4.3 Créer la base de données

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
```

### 4.5 Autoriser le service à lire ces secrets

```bash
NUMERO="$(gcloud projects describe depanio-prod --format='value(projectNumber)')"
COMPTE="${NUMERO}-compute@developer.gserviceaccount.com"

for secret in depanio-db-password depanio-jwt; do
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

### Créer le premier administrateur

Le jeu de démonstration n'existe que sous le profil `dev` : la base de production
démarre donc vide. Créez le compte initial directement en base :

```bash
gcloud sql connect depanio-sql --user=depanio --database=depanio
```

```sql
-- Remplacez l'empreinte par celle de votre mot de passe.
-- Générez-la avec un outil BCrypt, par exemple bcrypt-generator.com
INSERT INTO users (full_name, email, password, role, active, created_at)
VALUES ('Votre Nom', 'vous@depanio.tn',
        '$2a$10$REMPLACEZ_PAR_VOTRE_EMPREINTE_BCRYPT',
        'ADMIN', true, NOW());
```

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
