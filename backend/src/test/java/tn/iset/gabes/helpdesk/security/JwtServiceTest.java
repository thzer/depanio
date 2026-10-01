package tn.iset.gabes.helpdesk.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tn.iset.gabes.helpdesk.config.HelpdeskProperties;
import tn.iset.gabes.helpdesk.entity.Establishment;
import tn.iset.gabes.helpdesk.entity.User;
import tn.iset.gabes.helpdesk.entity.enums.Role;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifie la production et le controle des jetons JWT.
 * <p>
 * Le point essentiel est le dernier test : un jeton signe avec une autre
 * cle doit etre rejete. C'est ce qui empeche un attaquant de fabriquer
 * lui-meme un jeton en se declarant administrateur.
 */
@DisplayName("Service JWT")
class JwtServiceTest {

    private static final String SECRET =
            "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW9ubHktbm90LWZvci1wcm9kdWN0aW9u";
    private static final String OTHER_SECRET =
            "YW5vdGhlci1zZWNyZXQta2V5LXRoYXQtaXMtY29tcGxldGVseS1kaWZmZXJlbnQtaGVyZQ==";

    private JwtService jwtService;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(propertiesWithSecret(SECRET, 60_000L));

        Establishment establishment = new Establishment();
        establishment.setId(7L);
        establishment.setName("Cafe de test");

        User user = User.builder()
                .fullName("Gerant de test")
                .email("gerant@test.tn")
                .password("empreinte")
                .role(Role.CLIENT)
                .establishment(establishment)
                .active(true)
                .build();
        user.setId(42L);

        principal = new UserPrincipal(user);
    }

    @Test
    @DisplayName("le jeton d'acces transporte l'adresse et l'identifiant de l'utilisateur")
    void accessTokenCarriesIdentity() {
        String token = jwtService.generateAccessToken(principal);

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo("gerant@test.tn");
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("un jeton d'acces n'est pas un jeton de renouvellement")
    void accessTokenIsNotARefreshToken() {
        assertThat(jwtService.isRefreshToken(jwtService.generateAccessToken(principal)))
                .isFalse();
    }

    @Test
    @DisplayName("le jeton de renouvellement est correctement identifie")
    void refreshTokenIsRecognised() {
        String refreshToken = jwtService.generateRefreshToken(principal);

        assertThat(jwtService.isTokenValid(refreshToken)).isTrue();
        assertThat(jwtService.isRefreshToken(refreshToken)).isTrue();
    }

    @Test
    @DisplayName("un jeton expire est refuse")
    void expiredTokenIsRejected() throws InterruptedException {
        // Duree de validite de 1 ms : le jeton est perime des sa creation
        JwtService shortLived = new JwtService(propertiesWithSecret(SECRET, 1L));
        String token = shortLived.generateAccessToken(principal);

        Thread.sleep(50);

        assertThat(shortLived.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("un jeton signe avec une autre cle est refuse")
    void tokenSignedWithAnotherKeyIsRejected() {
        // Un attaquant fabrique un jeton avec sa propre cle...
        JwtService attacker = new JwtService(propertiesWithSecret(OTHER_SECRET, 60_000L));
        String forged = attacker.generateAccessToken(principal);

        // ... le serveur, qui connait la vraie cle, doit le rejeter
        assertThat(jwtService.isTokenValid(forged)).isFalse();
    }

    @Test
    @DisplayName("une chaine quelconque n'est pas acceptee comme jeton")
    void garbageIsRejected() {
        assertThat(jwtService.isTokenValid("ceci-nest-pas-un-jeton")).isFalse();
    }

    private HelpdeskProperties propertiesWithSecret(String secret, long expiration) {
        HelpdeskProperties properties = new HelpdeskProperties();
        properties.getSecurity().getJwt().setSecret(secret);
        properties.getSecurity().getJwt().setExpiration(expiration);
        properties.getSecurity().getJwt().setRefreshExpiration(expiration * 2);
        return properties;
    }
}
