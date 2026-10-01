package tn.iset.gabes.helpdesk.config;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tn.iset.gabes.helpdesk.entity.Equipment;
import tn.iset.gabes.helpdesk.entity.Establishment;
import tn.iset.gabes.helpdesk.entity.Ticket;
import tn.iset.gabes.helpdesk.entity.TicketComment;
import tn.iset.gabes.helpdesk.entity.TicketHistory;
import tn.iset.gabes.helpdesk.entity.User;
import tn.iset.gabes.helpdesk.entity.enums.EquipmentType;
import tn.iset.gabes.helpdesk.entity.enums.EstablishmentType;
import tn.iset.gabes.helpdesk.entity.enums.HistoryAction;
import tn.iset.gabes.helpdesk.entity.enums.Priority;
import tn.iset.gabes.helpdesk.entity.enums.Role;
import tn.iset.gabes.helpdesk.entity.enums.TicketCategory;
import tn.iset.gabes.helpdesk.entity.enums.TicketStatus;
import tn.iset.gabes.helpdesk.repository.EquipmentRepository;
import tn.iset.gabes.helpdesk.repository.EstablishmentRepository;
import tn.iset.gabes.helpdesk.repository.TicketRepository;
import tn.iset.gabes.helpdesk.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Jeu de donnees de demonstration.
 * <p>
 * Ce composant n'est actif que sous le profil {@code dev} et ne s'execute
 * que si la base est vide : il ne peut donc ni s'executer en production ni
 * ecraser des donnees existantes.
 * <p>
 * Il remplit l'application avec des etablissements, du materiel et des
 * tickets a differents stades de traitement, ce qui permet de presenter
 * toutes les fonctionnalites sans ressaisir des donnees a chaque
 * demarrage.
 * <p>
 * Les comptes crees et leurs mots de passe sont listes dans le fichier
 * README.md a la racine du projet.
 */
@Slf4j
@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder {

    /** Mot de passe commun aux comptes de demonstration. */
    private static final String DEMO_PASSWORD = "Passer@123";

    private final UserRepository userRepository;
    private final EstablishmentRepository establishmentRepository;
    private final EquipmentRepository equipmentRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;
    private final EntityManager entityManager;

    @Bean
    public ApplicationRunner seedDemoData() {
        return args -> {
            if (userRepository.count() > 0) {
                log.info("Base deja peuplee : le jeu de demonstration n'est pas recharge.");
                return;
            }
            log.info("Base vide : creation du jeu de donnees de demonstration...");

            // Une transaction explicite est ouverte ici : l'annotation
            // @Transactional serait sans effet, la methode etant appelee
            // directement depuis cette classe et non via le proxy Spring.
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> seed());

            log.info("Jeu de demonstration cree : {} comptes, {} etablissements, {} tickets.",
                    userRepository.count(),
                    establishmentRepository.count(),
                    ticketRepository.count());
        };
    }

    private void seed() {
        String encoded = passwordEncoder.encode(DEMO_PASSWORD);

        // ---------- Equipe de la societe de maintenance ----------
        User admin = userRepository.save(User.builder()
                .fullName("Mohamed Yahya Ben Yaflah")
                .email("admin@helpdesk.tn")
                .password(encoded)
                .phone("75 123 456")
                .role(Role.ADMIN)
                .active(true)
                .build());

        User techCaisse = userRepository.save(User.builder()
                .fullName("Ahmed Trabelsi")
                .email("ahmed.tech@helpdesk.tn")
                .password(encoded)
                .phone("98 111 222")
                .role(Role.TECHNICIAN)
                .speciality("Caisses et terminaux de paiement")
                .active(true)
                .build());

        User techFroid = userRepository.save(User.builder()
                .fullName("Salma Gharbi")
                .email("salma.tech@helpdesk.tn")
                .password(encoded)
                .phone("98 333 444")
                .role(Role.TECHNICIAN)
                .speciality("Froid et machines a cafe")
                .active(true)
                .build());

        User techReseau = userRepository.save(User.builder()
                .fullName("Karim Mansouri")
                .email("karim.tech@helpdesk.tn")
                .password(encoded)
                .phone("98 555 666")
                .role(Role.TECHNICIAN)
                .speciality("Reseau et informatique")
                .active(true)
                .build());

        // ---------- Etablissements clients ----------
        Establishment cafeElMedina = establishmentRepository.save(Establishment.builder()
                .name("Cafe El Medina")
                .type(EstablishmentType.CAFE)
                .address("Avenue Farhat Hached")
                .city("Gabes")
                .phone("75 270 100")
                .contactName("Hichem Bouazizi")
                .contactEmail("contact@elmedina.tn")
                .contractNumber("CT-2025-001")
                .active(true)
                .build());

        Establishment restaurantDarZaman = establishmentRepository.save(Establishment.builder()
                .name("Restaurant Dar Zaman")
                .type(EstablishmentType.RESTAURANT)
                .address("Rue de la Republique")
                .city("Gabes")
                .phone("75 271 200")
                .contactName("Nadia Jemli")
                .contactEmail("contact@darzaman.tn")
                .contractNumber("CT-2025-002")
                .active(true)
                .build());

        Establishment salonYasmine = establishmentRepository.save(Establishment.builder()
                .name("Salon de The Yasmine")
                .type(EstablishmentType.TEA_HOUSE)
                .address("Zone touristique")
                .city("Chenini")
                .phone("75 272 300")
                .contactName("Olfa Chaabane")
                .contactEmail("contact@yasmine.tn")
                .contractNumber("CT-2025-003")
                .active(true)
                .build());

        Establishment fastFoodOasis = establishmentRepository.save(Establishment.builder()
                .name("Oasis Fast Food")
                .type(EstablishmentType.FAST_FOOD)
                .address("Route de Medenine")
                .city("Gabes")
                .phone("75 273 400")
                .contactName("Sami Khelifi")
                .contactEmail("contact@oasisfood.tn")
                .contractNumber("CT-2025-004")
                .active(true)
                .build());

        // ---------- Comptes des gerants ----------
        User gerantMedina = userRepository.save(User.builder()
                .fullName("Hichem Bouazizi")
                .email("hichem@elmedina.tn")
                .password(encoded)
                .phone("22 100 100")
                .role(Role.CLIENT)
                .establishment(cafeElMedina)
                .active(true)
                .build());

        User gerantDarZaman = userRepository.save(User.builder()
                .fullName("Nadia Jemli")
                .email("nadia@darzaman.tn")
                .password(encoded)
                .phone("22 200 200")
                .role(Role.CLIENT)
                .establishment(restaurantDarZaman)
                .active(true)
                .build());

        User gerantYasmine = userRepository.save(User.builder()
                .fullName("Olfa Chaabane")
                .email("olfa@yasmine.tn")
                .password(encoded)
                .phone("22 300 300")
                .role(Role.CLIENT)
                .establishment(salonYasmine)
                .active(true)
                .build());

        User gerantOasis = userRepository.save(User.builder()
                .fullName("Sami Khelifi")
                .email("sami@oasisfood.tn")
                .password(encoded)
                .phone("22 400 400")
                .role(Role.CLIENT)
                .establishment(fastFoodOasis)
                .active(true)
                .build());

        // ---------- Materiel installe ----------
        Equipment caisseMedina = saveEquipment("Caisse comptoir",
                EquipmentType.POS_TERMINAL, "Sunmi", "T2 Lite", "SNM-4471",
                "Comptoir principal", cafeElMedina,
                LocalDate.of(2024, 3, 15), LocalDate.of(2026, 3, 15));

        Equipment machineCafeMedina = saveEquipment("Machine a cafe 2 groupes",
                EquipmentType.COFFEE_MACHINE, "La Cimbali", "M26", "CMB-9920",
                "Comptoir principal", cafeElMedina,
                LocalDate.of(2023, 11, 5), LocalDate.of(2025, 11, 5));

        Equipment tpeMedina = saveEquipment("Terminal de paiement",
                EquipmentType.CARD_READER, "Ingenico", "Move 5000", "ING-3310",
                "Comptoir principal", cafeElMedina, LocalDate.of(2024, 6, 1), null);

        Equipment caisseDarZaman = saveEquipment("Caisse salle",
                EquipmentType.POS_TERMINAL, "Sunmi", "T2", "SNM-5582",
                "Entree de la salle", restaurantDarZaman,
                LocalDate.of(2024, 1, 20), LocalDate.of(2026, 1, 20));

        Equipment imprimanteCuisine = saveEquipment("Imprimante bons de cuisine",
                EquipmentType.PRINTER, "Epson", "TM-T20III", "EPS-7741",
                "Cuisine", restaurantDarZaman, LocalDate.of(2024, 1, 20), null);

        Equipment frigoDarZaman = saveEquipment("Vitrine refrigeree",
                EquipmentType.REFRIGERATION, "Liebherr", "FKv 4143", "LBH-2214",
                "Cuisine", restaurantDarZaman,
                LocalDate.of(2023, 8, 10), LocalDate.of(2026, 8, 10));

        Equipment caisseYasmine = saveEquipment("Caisse tactile",
                EquipmentType.POS_TERMINAL, "Aures", "Yuno", "AUR-1180",
                "Comptoir", salonYasmine, LocalDate.of(2025, 2, 1),
                LocalDate.of(2027, 2, 1));

        Equipment routeurYasmine = saveEquipment("Routeur Wi-Fi",
                EquipmentType.NETWORK_DEVICE, "TP-Link", "Archer C6", "TPL-6650",
                "Bureau", salonYasmine, LocalDate.of(2024, 9, 12), null);

        Equipment caisseOasis = saveEquipment("Caisse rapide",
                EquipmentType.POS_TERMINAL, "Sunmi", "T2s", "SNM-7713",
                "Comptoir commande", fastFoodOasis,
                LocalDate.of(2025, 5, 3), LocalDate.of(2027, 5, 3));

        Equipment tabletteOasis = saveEquipment("Tablette de prise de commande",
                EquipmentType.TABLET, "Samsung", "Galaxy Tab A9", "SAM-9031",
                "Salle", fastFoodOasis, LocalDate.of(2025, 5, 3), null);

        // ---------- Tickets a differents stades ----------

        // 1. Nouveau, critique, non affecte : apparait en tete de la file d'attente
        saveTicket("Caisse bloquee, impossible d'encaisser",
                "Depuis ce matin la caisse affiche un ecran noir au demarrage. "
                        + "Nous ne pouvons plus encaisser les clients et la file "
                        + "d'attente s'allonge. Intervention urgente demandee.",
                TicketCategory.HARDWARE_FAILURE, Priority.CRITICAL, TicketStatus.NEW,
                cafeElMedina, caisseMedina, gerantMedina, null,
                LocalDateTime.now().minusHours(1), null, null);

        // 2. Affecte, haute priorite
        Ticket ticketTpe = saveTicket("Terminal de paiement refuse les cartes",
                "Le terminal affiche erreur de connexion a chaque paiement par carte. "
                        + "Les paiements en especes fonctionnent normalement.",
                TicketCategory.NETWORK, Priority.HIGH, TicketStatus.ASSIGNED,
                cafeElMedina, tpeMedina, gerantMedina, techCaisse,
                LocalDateTime.now().minusHours(4),
                LocalDateTime.now().minusHours(3), null);
        addComment(ticketTpe, techCaisse,
                "Bonjour, je passe cet apres-midi vers 15h pour verifier la "
                        + "configuration reseau du terminal.", false);

        // 3. En cours de traitement, avec note interne
        Ticket ticketFroid = saveTicket("Vitrine refrigeree ne descend plus en temperature",
                "La vitrine reste a 12 degres au lieu de 4. Nous avons sorti les "
                        + "produits frais par precaution.",
                TicketCategory.HARDWARE_FAILURE, Priority.CRITICAL, TicketStatus.IN_PROGRESS,
                restaurantDarZaman, frigoDarZaman, gerantDarZaman, techFroid,
                LocalDateTime.now().minusHours(6),
                LocalDateTime.now().minusHours(5), null);
        addComment(ticketFroid, techFroid,
                "Je suis sur place. Le condenseur est tres encrasse, je procede "
                        + "au nettoyage complet.", false);
        addComment(ticketFroid, techFroid,
                "Note interne : prevoir un devis de remplacement du ventilateur, "
                        + "il montre des signes d'usure avancee.", true);

        // 4. En attente du client
        saveTicket("Imprimante de cuisine ne sort plus les bons",
                "Les commandes ne s'impriment plus en cuisine depuis hier soir.",
                TicketCategory.HARDWARE_FAILURE, Priority.HIGH, TicketStatus.WAITING_CLIENT,
                restaurantDarZaman, imprimanteCuisine, gerantDarZaman, techCaisse,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1).plusHours(1), null);

        // 5. Resolu, en attente de confirmation du client
        Ticket ticketResolu = saveTicket("Wi-Fi instable en salle",
                "La connexion Wi-Fi se coupe reguliErement, ce qui bloque la "
                        + "tablette de prise de commande.",
                TicketCategory.NETWORK, Priority.MEDIUM, TicketStatus.RESOLVED,
                salonYasmine, routeurYasmine, gerantYasmine, techReseau,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(2).plusHours(2),
                LocalDateTime.now().minusHours(20));
        ticketResolu.setResolutionNote(
                "Mise a jour du micrologiciel du routeur et changement du canal "
                        + "Wi-Fi vers un canal moins sature. Debit teste et stable "
                        + "pendant trente minutes.");
        ticketRepository.save(ticketResolu);

        // 6. Cloture avec note de satisfaction : alimente le delai moyen de resolution
        Ticket ticketClos = saveTicket("Machine a cafe fuit au niveau du groupe",
                "Une fuite d'eau apparait sous le groupe de droite pendant "
                        + "l'extraction.",
                TicketCategory.HARDWARE_FAILURE, Priority.HIGH, TicketStatus.CLOSED,
                cafeElMedina, machineCafeMedina, gerantMedina, techFroid,
                LocalDateTime.now().minusDays(8),
                LocalDateTime.now().minusDays(8).plusMinutes(45),
                LocalDateTime.now().minusDays(8).plusHours(5));
        ticketClos.setResolutionNote(
                "Remplacement du joint de groupe et detartrage complet du circuit.");
        ticketClos.setClosedAt(LocalDateTime.now().minusDays(7));
        ticketClos.setSatisfactionRating(5);
        ticketRepository.save(ticketClos);

        // 7. Ticket dont le delai est deja depasse : illustre le suivi des SLA
        Ticket ticketEnRetard = saveTicket("Tablette de commande ne se charge plus",
                "La tablette ne tient plus la charge, elle s'eteint au bout de "
                        + "quinze minutes.",
                TicketCategory.HARDWARE_FAILURE, Priority.MEDIUM, TicketStatus.IN_PROGRESS,
                fastFoodOasis, tabletteOasis, gerantOasis, techCaisse,
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusDays(3).plusHours(2), null);
        ticketEnRetard.setSlaDueAt(LocalDateTime.now().minusHours(30));
        ticketEnRetard.setSlaBreached(true);
        ticketEnRetard.addHistoryEntry(TicketHistory.builder()
                .action(HistoryAction.SLA_BREACHED)
                .comment("Depassement constate automatiquement")
                .build());
        ticketRepository.save(ticketEnRetard);

        // 8. Demandes de priorite basse
        saveTicket("Formation du nouveau serveur sur la caisse",
                "Nous avons recrute un serveur, nous souhaitons une session de "
                        + "formation d'une heure sur l'utilisation de la caisse.",
                TicketCategory.TRAINING, Priority.LOW, TicketStatus.NEW,
                fastFoodOasis, caisseOasis, gerantOasis, null,
                LocalDateTime.now().minusHours(10), null, null);

        saveTicket("Ajouter de nouveaux articles au menu de la caisse",
                "Nous souhaitons ajouter cinq nouveaux desserts dans le menu "
                        + "enregistre sur la caisse.",
                TicketCategory.CONFIGURATION, Priority.LOW, TicketStatus.ASSIGNED,
                salonYasmine, caisseYasmine, gerantYasmine, techCaisse,
                LocalDateTime.now().minusHours(26),
                LocalDateTime.now().minusHours(24), null);

        saveTicket("Rouleaux de papier pour imprimante caisse",
                "Nous arrivons a la fin de notre stock de rouleaux thermiques.",
                TicketCategory.CONSUMABLE, Priority.LOW, TicketStatus.NEW,
                restaurantDarZaman, caisseDarZaman, gerantDarZaman, null,
                LocalDateTime.now().minusHours(3), null, null);

        // 9. Tickets clotures plus anciens, pour alimenter la courbe mensuelle
        for (int monthsAgo = 1; monthsAgo <= 5; monthsAgo++) {
            Ticket historique = saveTicket(
                    "Maintenance preventive mensuelle",
                    "Visite de controle et nettoyage du materiel sous contrat.",
                    TicketCategory.OTHER, Priority.LOW, TicketStatus.CLOSED,
                    cafeElMedina, caisseMedina, gerantMedina, techCaisse,
                    LocalDateTime.now().minusMonths(monthsAgo),
                    LocalDateTime.now().minusMonths(monthsAgo).plusHours(1),
                    LocalDateTime.now().minusMonths(monthsAgo).plusHours(3));
            historique.setResolutionNote("Controle effectue, aucun defaut constate.");
            historique.setClosedAt(
                    LocalDateTime.now().minusMonths(monthsAgo).plusHours(4));
            historique.setSatisfactionRating(4);
            ticketRepository.save(historique);
        }
    }

    // ------------------------------------------------------------------
    //  Fabriques internes
    // ------------------------------------------------------------------

    private Equipment saveEquipment(String name, EquipmentType type, String brand,
                                    String model, String serial, String location,
                                    Establishment establishment,
                                    LocalDate installedAt, LocalDate warrantyEnd) {
        return equipmentRepository.save(Equipment.builder()
                .name(name)
                .type(type)
                .brand(brand)
                .model(model)
                .serialNumber(serial)
                .location(location)
                .qrToken(UUID.randomUUID().toString())
                .establishment(establishment)
                .installedAt(installedAt)
                .warrantyEnd(warrantyEnd)
                .active(true)
                .build());
    }

    /**
     * Cree un ticket de demonstration a une date choisie.
     * <p>
     * L'audit JPA positionne {@code created_at} a l'instant du demarrage ;
     * sans correction, tous les tickets auraient le meme age et la courbe
     * mensuelle du tableau de bord serait vide. La colonne etant declaree
     * non modifiable, Hibernate ne la met jamais a jour : la date est donc
     * repositionnee par une requete SQL directe, uniquement dans ce jeu de
     * demonstration.
     */
    private Ticket saveTicket(String title, String description,
                              TicketCategory category, Priority priority,
                              TicketStatus status,
                              Establishment establishment, Equipment equipment,
                              User createdBy, User assignedTo,
                              LocalDateTime createdAt, LocalDateTime assignedAt,
                              LocalDateTime resolvedAt) {

        int slaHours = switch (priority) {
            case CRITICAL -> 2;
            case HIGH -> 8;
            case MEDIUM -> 24;
            case LOW -> 72;
        };

        Ticket ticket = Ticket.builder()
                .reference(nextReference())
                .title(title)
                .description(description)
                .category(category)
                .priority(priority)
                .status(status)
                .establishment(establishment)
                .equipment(equipment)
                .createdBy(createdBy)
                .assignedTo(assignedTo)
                .assignedAt(assignedAt)
                .resolvedAt(resolvedAt)
                .slaDueAt(createdAt.plusHours(slaHours))
                .build();

        ticket.addHistoryEntry(TicketHistory.builder()
                .action(HistoryAction.CREATED)
                .newValue(TicketStatus.NEW.getLabel())
                .actor(createdBy)
                .build());

        if (assignedTo != null) {
            ticket.addHistoryEntry(TicketHistory.builder()
                    .action(HistoryAction.ASSIGNED)
                    .newValue(assignedTo.getFullName())
                    .build());
        }

        Ticket saved = ticketRepository.saveAndFlush(ticket);
        forceCreationDate(saved.getId(), createdAt);
        return saved;
    }

    /**
     * Repositionne la date de creation d'un ticket en base.
     * Reserve au jeu de demonstration.
     */
    private void forceCreationDate(Long ticketId, LocalDateTime createdAt) {
        entityManager.createNativeQuery(
                        "UPDATE tickets SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", ticketId)
                .executeUpdate();
    }

    private void addComment(Ticket ticket, User author, String content, boolean internal) {
        ticket.addComment(TicketComment.builder()
                .content(content)
                .author(author)
                .internalNote(internal)
                .build());
        ticketRepository.save(ticket);
    }

    /** Reference sequentielle du type TK-2026-0001. */
    private String nextReference() {
        String prefix = "TK-" + LocalDate.now().getYear() + "-";
        long count = ticketRepository.count();
        return prefix + String.format("%04d", count + 1);
    }
}
