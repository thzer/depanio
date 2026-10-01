#!/usr/bin/env bash
# =============================================================================
#  Depanio — deploiement sur Google Cloud Run
#
#  Ce script construit l image, la publie, puis met en service la nouvelle
#  version. Il est volontairement idempotent : le relancer apres une
#  modification du code met simplement l application a jour.
#
#  Prerequis, decrits en detail dans docs/03-deploiement.md :
#    - gcloud installe et authentifie   (gcloud auth login)
#    - un projet Google Cloud avec la facturation activee
#    - une instance Cloud SQL et un secret JWT crees (voir le guide)
#
#  Utilisation :  ./deploy/deploy.sh
# =============================================================================

set -euo pipefail

# ----------------------------------------------------------- Configuration
# Ces valeurs se surchargent par variables d environnement, ce qui permet
# d utiliser le meme script pour un environnement de recette.

PROJET="${GCP_PROJECT:?Definissez GCP_PROJECT avec l identifiant de votre projet Google Cloud}"
REGION="${GCP_REGION:-europe-west1}"
SERVICE="${SERVICE_NAME:-depanio}"

INSTANCE_SQL="${SQL_INSTANCE:-depanio-sql}"
BASE="${SQL_DATABASE:-depanio}"
UTILISATEUR_SQL="${SQL_USER:-depanio}"

SECRET_JWT="${JWT_SECRET_NAME:-depanio-jwt}"
SECRET_MDP="${DB_PASSWORD_SECRET:-depanio-db-password}"
SECRET_ADMIN="${ADMIN_PASSWORD_SECRET:-depanio-admin-password}"

# Compte administrateur cree au tout premier demarrage, sur base vide.
# Passe ce premier lancement, ces valeurs n ont plus aucun effet.
ADMIN_EMAIL="${ADMIN_EMAIL:?Definissez ADMIN_EMAIL avec l adresse de votre compte administrateur}"

IMAGE="${REGION}-docker.pkg.dev/${PROJET}/depanio/${SERVICE}"

# ------------------------------------------------------------- Affichage

bleu() { printf '\033[1;36m%s\033[0m\n' "$1"; }
vert() { printf '\033[1;32m%s\033[0m\n' "$1"; }

bleu "=============================================="
bleu " Deploiement de Depanio"
bleu "=============================================="
echo "  Projet  : ${PROJET}"
echo "  Region  : ${REGION}"
echo "  Service : ${SERVICE}"
echo

# --------------------------------------------------- 1. Depot d images
# Cree au premier passage seulement ; les fois suivantes la commande
# echoue sans consequence, d ou le || true.

bleu "[1/4] Preparation du depot d images"
gcloud artifacts repositories create depanio \
  --repository-format=docker \
  --location="${REGION}" \
  --description="Images de l application Depanio" \
  --project="${PROJET}" 2>/dev/null || echo "  Le depot existe deja."

gcloud auth configure-docker "${REGION}-docker.pkg.dev" --quiet

# ------------------------------------------------------- 2. Construction
# La construction a lieu sur Cloud Build, pas sur le poste : inutile
# d installer Docker en local, et le resultat est identique a chaque fois.

bleu "[2/4] Construction de l image sur Cloud Build"
ETIQUETTE="$(date +%Y%m%d-%H%M%S)"
gcloud builds submit \
  --tag "${IMAGE}:${ETIQUETTE}" \
  --project="${PROJET}" \
  --region="${REGION}" \
  .

# Une etiquette horodatee permet de revenir a une version precedente ;
# l etiquette latest sert de raccourci de lecture.
gcloud artifacts docker tags add \
  "${IMAGE}:${ETIQUETTE}" "${IMAGE}:latest" --quiet 2>/dev/null || true

# ----------------------------------------------------- 3. Mise en service

bleu "[3/4] Mise en service sur Cloud Run"

CONNEXION_SQL="${PROJET}:${REGION}:${INSTANCE_SQL}"

# La connexion passe par une socket Unix : la base n est jamais exposee
# sur l Internet public, et aucun mot de passe ne transite par le reseau.
URL_BASE="jdbc:mysql:///${BASE}?cloudSqlInstance=${CONNEXION_SQL}&socketFactory=com.google.cloud.sql.mysql.SocketFactory&useSSL=false&serverTimezone=Africa/Tunis&characterEncoding=UTF-8"

gcloud run deploy "${SERVICE}" \
  --image "${IMAGE}:${ETIQUETTE}" \
  --region "${REGION}" \
  --project "${PROJET}" \
  --platform managed \
  --allow-unauthenticated \
  --port 8080 \
  --memory 1Gi \
  --cpu 1 \
  --min-instances 0 \
  --max-instances 4 \
  --timeout 300 \
  --add-cloudsql-instances "${CONNEXION_SQL}" \
  --set-env-vars "SPRING_PROFILES_ACTIVE=prod" \
  --set-env-vars "DB_URL=${URL_BASE}" \
  --set-env-vars "DB_USERNAME=${UTILISATEUR_SQL}" \
  --set-env-vars "JPA_DDL_AUTO=update" \
  --set-secrets "DB_PASSWORD=${SECRET_MDP}:latest" \
  --set-secrets "JWT_SECRET=${SECRET_JWT}:latest" \
  --set-env-vars "HELPDESK_ADMIN_EMAIL=${ADMIN_EMAIL}" \
  --set-secrets "HELPDESK_ADMIN_PASSWORD=${SECRET_ADMIN}:latest"

# ------------------------------------------------- 4. Adresse publique
# L adresse n est connue qu apres le premier deploiement : elle est donc
# relue puis reinjectee, afin que les etiquettes QR pointent au bon endroit.

bleu "[4/4] Alignement de l adresse publique"

ADRESSE="$(gcloud run services describe "${SERVICE}" \
  --region "${REGION}" --project "${PROJET}" --format 'value(status.url)')"

gcloud run services update "${SERVICE}" \
  --region "${REGION}" --project "${PROJET}" \
  --set-env-vars "FRONTEND_URL=${ADRESSE}" \
  --quiet > /dev/null

echo
vert "=============================================="
vert " Deploiement termine"
vert "=============================================="
echo "  Application : ${ADRESSE}"
echo "  Version     : ${ETIQUETTE}"
echo
echo "  Journaux :"
echo "    gcloud run services logs tail ${SERVICE} --region ${REGION} --project ${PROJET}"
echo
echo "  Revenir a la version precedente :"
echo "    gcloud run services update-traffic ${SERVICE} --to-revisions=PREVIOUS=100 --region ${REGION}"
