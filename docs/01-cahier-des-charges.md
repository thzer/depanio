# Cahier des charges

**Projet :** Système de gestion des tickets d'assistance technique pour cafés et restaurants
**Établissement :** ISET Gabès
**Nature :** Projet de Fin d'Études

---

## 1. Contexte

Les cafés et les restaurants dépendent aujourd'hui d'un parc d'équipements dont
l'arrêt interrompt immédiatement l'activité : caisse enregistreuse, terminal de
paiement par carte, imprimante de bons de cuisine, machine à café, matériel
frigorifique. Une caisse hors service en plein service de midi, c'est une file
d'attente qui s'allonge et des clients qui repartent.

Ces établissements confient l'entretien de ce matériel à une société de
maintenance, liée à eux par un contrat. C'est cette société qui est l'utilisateur
principal de l'application étudiée ici.

## 2. Problématique

Dans la pratique observée, les demandes d'intervention circulent par téléphone et
par messagerie instantanée. Ce fonctionnement présente quatre faiblesses :

| Faiblesse | Conséquence concrète |
|---|---|
| **Aucune trace écrite** | Un appel reçu pendant un déplacement n'est noté nulle part et se perd. |
| **Aucune priorisation** | Une caisse hors service est traitée dans le même ordre qu'une demande de modification de menu. |
| **Aucun engagement mesurable** | Ni le client ni la société ne peuvent dire combien de temps une panne a mis à être résolue. |
| **Aucun historique** | Impossible de savoir qu'une même machine est tombée en panne quatre fois ce trimestre. |

## 3. Objectif

Concevoir et réaliser une application web qui remplace ce fonctionnement oral par
un circuit **écrit, priorisé, mesuré et traçable**, depuis la déclaration de la
panne jusqu'à la confirmation de sa résolution par le client.

## 4. Acteurs

| Acteur | Description | Objectifs |
|---|---|---|
| **Administrateur** | Responsable de la société de maintenance | Gérer les clients, le matériel et les comptes ; répartir les tickets ; suivre les indicateurs |
| **Technicien** | Intervenant de la société | Consulter les tickets qui lui sont affectés, les traiter, documenter la résolution |
| **Client** | Gérant d'un café ou d'un restaurant | Déclarer une panne, suivre son avancement, échanger, confirmer la résolution |

## 5. Besoins fonctionnels

### 5.1 Gestion des tickets

| Réf. | Besoin |
|---|---|
| BF-01 | Un client déclare un problème en précisant son objet, sa description, sa nature et son urgence |
| BF-02 | Le système attribue automatiquement une référence unique lisible (TK-2026-0001) |
| BF-03 | Le système calcule l'échéance de résolution à partir de l'urgence déclarée |
| BF-04 | Un administrateur affecte le ticket à un technicien ; un technicien peut prendre en charge un ticket libre |
| BF-05 | Le ticket évolue selon un cycle de vie contrôlé, les transitions incohérentes étant refusées |
| BF-06 | La résolution exige la description de la solution appliquée |
| BF-07 | Le client confirme la clôture et attribue une note de satisfaction |
| BF-08 | Client et technicien échangent des messages rattachés au ticket |
| BF-09 | L'équipe technique dispose de notes internes, invisibles par le client |
| BF-10 | Chaque action est inscrite dans un historique immuable |

### 5.2 Délais contractuels (SLA)

| Réf. | Besoin |
|---|---|
| BF-11 | Chaque niveau d'urgence correspond à un délai de résolution négocié |
| BF-12 | Le technicien est alerté avant l'échéance d'un ticket qui lui est affecté |
| BF-13 | Le dépassement est constaté automatiquement et notifié aux administrateurs |
| BF-14 | Le décompte s'arrête à la résolution, l'engagement portant sur le temps de résolution |

### 5.3 Parc matériel et étiquettes QR

| Réf. | Besoin |
|---|---|
| BF-15 | Le matériel installé chez chaque client est inventorié avec sa garantie |
| BF-16 | Chaque machine porte une étiquette QR imprimable |
| BF-17 | Scanner l'étiquette ouvre un formulaire de déclaration déjà renseigné |
| BF-18 | Le système identifie le matériel le plus souvent en panne |

### 5.4 Notifications

| Réf. | Besoin |
|---|---|
| BF-19 | Les événements concernant un utilisateur lui sont notifiés en temps réel |
| BF-20 | Les notifications restent consultables après reconnexion |

### 5.5 Pilotage

| Réf. | Besoin |
|---|---|
| BF-21 | Un tableau de bord présente les indicateurs clés et leur évolution |
| BF-22 | L'administrateur visualise la charge de chaque technicien |
| BF-23 | Un client ne voit que les indicateurs de son propre établissement |

## 6. Besoins non fonctionnels

| Réf. | Besoin | Mise en œuvre retenue |
|---|---|---|
| BNF-01 | **Cloisonnement des données** : deux clients concurrents utilisent le même serveur sans jamais se voir | Filtre appliqué dans la couche service à partir de l'établissement du compte, jamais à partir d'un paramètre de requête |
| BNF-02 | **Authentification** sans session serveur | Jeton JWT signé, transmis à chaque appel |
| BNF-03 | **Confidentialité des mots de passe** | Empreinte BCrypt avec sel ; le mot de passe en clair n'est jamais stocké |
| BNF-04 | **Discrétion des erreurs** : ne pas révéler l'existence d'une ressource inaccessible | Réponse 404 et non 403 lorsqu'un client demande le ticket d'un autre |
| BNF-05 | **Performance** des listes et du tableau de bord | Pagination systématique, agrégats calculés en base, index sur les colonnes filtrées |
| BNF-06 | **Portabilité** du déploiement | Toutes les valeurs sensibles surchargeables par variables d'environnement |
| BNF-07 | **Utilisation sur téléphone** par le gérant en salle | Interface adaptative ; déclaration par QR code |
| BNF-08 | **Langue** | Interface entièrement en français |

## 7. Règles de gestion

| Réf. | Règle |
|---|---|
| RG-01 | Un ticket appartient à un seul établissement, déterminé à sa création et non modifiable |
| RG-02 | Seules les transitions déclarées dans le cycle de vie sont acceptées |
| RG-03 | La résolution d'un ticket exige une note décrivant la solution |
| RG-04 | Seul un ticket résolu peut être clôturé |
| RG-05 | Un ticket clôturé ou annulé n'est plus modifiable |
| RG-06 | Un technicien n'agit que sur les tickets qui lui sont affectés |
| RG-07 | Seul un administrateur peut réaffecter un ticket déjà pris en charge |
| RG-08 | Un matériel rattaché à un ticket doit appartenir à l'établissement de ce ticket |
| RG-09 | Un compte n'est jamais supprimé mais désactivé, afin de préserver l'historique |
| RG-10 | Le dernier administrateur actif ne peut pas être désactivé |
| RG-11 | Un technicien ayant des tickets en cours ne peut pas être désactivé |
| RG-12 | Un établissement ayant des tickets ouverts ne peut pas être désactivé |
| RG-13 | Une note interne ne peut être rédigée que par l'équipe technique |

## 8. Délais de résolution contractuels

| Priorité | Délai | Situation type |
|---|---|---|
| Critique | 2 heures | Caisse hors service, activité bloquée |
| Haute | 8 heures | Terminal de paiement en panne, encaissement dégradé |
| Moyenne | 24 heures | Wi-Fi instable, gêne sans blocage |
| Basse | 72 heures | Formation, ajout d'articles au menu, consommables |

Ces durées sont paramétrables sans recompilation afin de s'adapter aux termes de
chaque contrat.

## 9. Cycle de vie d'un ticket

```
NOUVEAU ──┬──► AFFECTÉ ──┬──► EN COURS ──┬──► EN ATTENTE DU CLIENT ──┐
          │              │               │                            │
          │              │               └──► RÉSOLU ◄────────────────┘
          │              │                      │  ▲
          │              │                      │  └── réouverture si
          │              │                      ▼      solution incomplète
          │              │                   CLÔTURÉ
          │              │
          └──────────────┴──► ANNULÉ
```

## 10. Périmètre exclu

Les éléments suivants sont volontairement hors du cadre de ce projet :

- la facturation des interventions et la gestion comptable ;
- la gestion des stocks de pièces détachées ;
- une application mobile native (l'interface web adaptative tient ce rôle) ;
- l'intégration avec les logiciels de caisse des clients.

---

ISET Gabès — Projet de Fin d'Études
