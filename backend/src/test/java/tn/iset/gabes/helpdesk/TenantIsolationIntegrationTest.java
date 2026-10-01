package tn.iset.gabes.helpdesk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.gabes.helpdesk.entity.Establishment;
import tn.iset.gabes.helpdesk.entity.Ticket;
import tn.iset.gabes.helpdesk.entity.User;
import tn.iset.gabes.helpdesk.entity.enums.EstablishmentType;
import tn.iset.gabes.helpdesk.entity.enums.Priority;
import tn.iset.gabes.helpdesk.entity.enums.Role;
import tn.iset.gabes.helpdesk.entity.enums.TicketCategory;
import tn.iset.gabes.helpdesk.entity.enums.TicketStatus;
import tn.iset.gabes.helpdesk.repository.EstablishmentRepository;
import tn.iset.gabes.helpdesk.repository.TicketRepository;
import tn.iset.gabes.helpdesk.repository.UserRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifie de bout en bout le cloisonnement des donnees entre clients.
 * <p>
 * C'est la garantie la plus sensible de l'application : deux cafes
 * concurrents utilisent le meme serveur, et aucun ne doit pouvoir
 * consulter les tickets de l'autre. Le test attaque l'API par de vraies
 * requetes HTTP, avec de vrais jetons, pour s'assurer que la regle tient
 * au niveau du point d'entree et pas seulement dans le code interne.
 * <p>
 * Le test s'execute sur une base H2 en memoire : il ne touche pas la base
 * de developpement et ne demande aucun serveur MySQL demarre.
 */
@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@Transactional
@DisplayName("Cloisonnement des donnees entre etablissements clients")
class TenantIsolationIntegrationTest {

    private static final String PASSWORD = "Passer@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EstablishmentRepository establishmentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Le canal WebSocket est remplace par un substitut : les tests portent
     * sur les regles d'acces, pas sur la diffusion temps reel.
     */
    @MockitoBean
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    private Long ticketOfFirstClient;

    @BeforeEach
    void setUp() {
        String encoded = passwordEncoder.encode(PASSWORD);

        Establishment first = establishmentRepository.save(Establishment.builder()
                .name("Cafe Premier")
                .type(EstablishmentType.CAFE)
                .city("Gabes")
                .active(true)
                .build());

        Establishment second = establishmentRepository.save(Establishment.builder()
                .name("Cafe Second")
                .type(EstablishmentType.CAFE)
                .city("Gabes")
                .active(true)
                .build());

        User managerOfFirst = userRepository.save(User.builder()
                .fullName("Gerant Premier")
                .email("premier@test.tn")
                .password(encoded)
                .role(Role.CLIENT)
                .establishment(first)
                .active(true)
                .build());

        userRepository.save(User.builder()
                .fullName("Gerant Second")
                .email("second@test.tn")
                .password(encoded)
                .role(Role.CLIENT)
                .establishment(second)
                .active(true)
                .build());

        userRepository.save(User.builder()
                .fullName("Administrateur")
                .email("admin@test.tn")
                .password(encoded)
                .role(Role.ADMIN)
                .active(true)
                .build());

        Ticket ticket = ticketRepository.save(Ticket.builder()
                .reference("TK-TEST-0001")
                .title("Caisse en panne chez le premier client")
                .description("Description du probleme rencontre par le premier client.")
                .category(TicketCategory.HARDWARE_FAILURE)
                .priority(Priority.HIGH)
                .status(TicketStatus.NEW)
                .establishment(first)
                .createdBy(managerOfFirst)
                .slaDueAt(LocalDateTime.now().plusHours(8))
                .build());

        ticketOfFirstClient = ticket.getId();
    }

    @Test
    @DisplayName("chaque client ne voit que ses propres tickets")
    void eachClientSeesOnlyItsOwnTickets() throws Exception {
        JsonNode firstClientView = getJson("/api/tickets", login("premier@test.tn"));
        JsonNode secondClientView = getJson("/api/tickets", login("second@test.tn"));

        assertThat(firstClientView.get("totalElements").asLong()).isEqualTo(1);
        assertThat(firstClientView.get("content").get(0).get("establishmentName").asText())
                .isEqualTo("Cafe Premier");

        assertThat(secondClientView.get("totalElements").asLong()).isZero();
    }

    @Test
    @DisplayName("l'administrateur voit les tickets de tous les etablissements")
    void administratorSeesEverything() throws Exception {
        JsonNode adminView = getJson("/api/tickets", login("admin@test.tn"));

        assertThat(adminView.get("totalElements").asLong()).isEqualTo(1);
    }

    @Test
    @DisplayName("forcer le filtre d'etablissement ne contourne pas le cloisonnement")
    void forcingTheEstablishmentFilterChangesNothing() throws Exception {
        Long otherEstablishmentId = establishmentRepository.findByActiveTrue().stream()
                .filter(establishment -> establishment.getName().equals("Cafe Premier"))
                .findFirst()
                .orElseThrow()
                .getId();

        // Le second client demande explicitement les tickets du premier
        JsonNode response = getJson(
                "/api/tickets?establishmentId=" + otherEstablishmentId,
                login("second@test.tn"));

        // Le parametre est ignore : il ne recoit rien
        assertThat(response.get("totalElements").asLong()).isZero();
    }

    @Test
    @DisplayName("lire le ticket d'un autre client renvoie 404 et non 403")
    void readingAnotherClientTicketReturnsNotFound() throws Exception {
        // Un 403 confirmerait au demandeur que ce ticket existe :
        // le serveur repond donc comme s'il n'existait pas du tout
        mockMvc.perform(get("/api/tickets/" + ticketOfFirstClient)
                        .header("Authorization", "Bearer " + login("second@test.tn")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("le proprietaire du ticket peut le consulter")
    void ownerCanReadItsTicket() throws Exception {
        mockMvc.perform(get("/api/tickets/" + ticketOfFirstClient)
                        .header("Authorization", "Bearer " + login("premier@test.tn")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("un client ne peut pas acceder a l'administration des comptes")
    void clientCannotReachUserAdministration() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + login("premier@test.tn")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("sans jeton, l'API refuse l'acces")
    void anonymousAccessIsRejected() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un mot de passe errone ne delivre aucun jeton")
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"premier@test.tn","password":"mauvais"}"""))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    //  Utilitaires de test
    // ------------------------------------------------------------------

    /** Authentifie un compte et renvoie son jeton d'acces. */
    private String login(String email) throws Exception {
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("email", email, "password", PASSWORD));

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private JsonNode getJson(String url, String token) throws Exception {
        String response = mockMvc.perform(get(url)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }
}
