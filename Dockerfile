# =============================================================================
#  Depanio — image de production
#
#  Construction en trois etapes, pour que l'image finale ne contienne ni
#  Node, ni Maven, ni le code source : seulement un JRE et l'archive
#  executable. On passe ainsi d'environ 900 Mo a moins de 250 Mo, ce qui
#  accelere chaque deploiement et reduit la surface exposee.
#
#  L'application Angular compilee est placee dans les ressources statiques
#  de Spring Boot : une seule image, une seule adresse, aucune
#  configuration de partage entre domaines.
# =============================================================================

# ---------------------------------------------------------------- Etape 1/3
#  Compilation du frontend Angular
# ---------------------------------------------------------------------------
FROM node:22-alpine AS frontend

WORKDIR /build

# Les dependances sont installees avant de copier le code : tant que
# package-lock.json ne change pas, Docker reutilise cette couche et
# l'installation n'est pas rejouee a chaque modification du code.
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY frontend/ ./
RUN npm run build -- --configuration production

# ---------------------------------------------------------------- Etape 2/3
#  Compilation du backend Spring Boot
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS backend

WORKDIR /build

# Meme principe : les dependances Maven d'abord, le code ensuite
COPY backend/pom.xml ./
RUN mvn -B dependency:go-offline

COPY backend/src ./src

# Le frontend compile rejoint les ressources servies par Spring Boot
COPY --from=frontend /build/dist/frontend/browser/ ./src/main/resources/static/

# Les tests sont executes par l'integration continue, pas pendant la
# construction de l'image : cela garde les deploiements rapides.
RUN mvn -B -DskipTests package

# ---------------------------------------------------------------- Etape 3/3
#  Image d'execution
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime

# L'application ne tourne pas en tant que root : si un defaut permettait
# d'executer du code, celui-ci n'aurait aucun privilege sur le systeme.
RUN addgroup -S depanio && adduser -S depanio -G depanio

WORKDIR /app
COPY --from=backend /build/target/helpdesk-*.jar app.jar
RUN chown -R depanio:depanio /app

USER depanio

# Cloud Run impose le port d'ecoute par la variable PORT
ENV PORT=8080
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod

# MaxRAMPercentage plutot qu'une taille fixe : la JVM s'adapte ainsi a la
# memoire reellement allouee au conteneur, quelle que soit la formule
# choisie sur Cloud Run.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
