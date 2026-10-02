package tn.iset.gabes.helpdesk.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifie la protection contre les essais de mot de passe en rafale.
 * <p>
 * Sans elle, un programme peut essayer des milliers de combinaisons sur
 * une adresse connue. C'est l'attaque la plus simple a mener contre une
 * application exposee : ces tests existent pour qu'une evolution ne
 * desactive pas la protection sans que personne ne le remarque.
 */
@DisplayName("Protection contre les essais en rafale")
class LoginAttemptServiceTest {

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService();
    }

    @Test
    @DisplayName("quelques erreurs ne bloquent pas un utilisateur distrait")
    void quelquesErreursNeBloquentPas() {
        for (int i = 0; i < 3; i++) {
            service.echec("gerant@cafe.tn");
        }
        assertThat(service.estBloque("gerant@cafe.tn")).isFalse();
    }

    @Test
    @DisplayName("les essais repetes finissent par etre bloques")
    void lesEssaisRepetesSontBloques() {
        for (int i = 0; i < 6; i++) {
            service.echec("cible@cafe.tn");
        }
        assertThat(service.estBloque("cible@cafe.tn")).isTrue();
        assertThat(service.minutesRestantes("cible@cafe.tn")).isPositive();
    }

    @Test
    @DisplayName("une connexion reussie efface les echecs precedents")
    void laReussiteEffaceLesEchecs() {
        for (int i = 0; i < 5; i++) {
            service.echec("gerant@cafe.tn");
        }
        service.reussite("gerant@cafe.tn");

        // Le compteur etant remis a zero, cinq nouveaux echecs ne
        // suffisent pas a bloquer
        for (int i = 0; i < 5; i++) {
            service.echec("gerant@cafe.tn");
        }
        assertThat(service.estBloque("gerant@cafe.tn")).isFalse();
    }

    @Test
    @DisplayName("bloquer un compte n'affecte pas les autres")
    void leBlocageEstPropreAChaqueCle() {
        for (int i = 0; i < 6; i++) {
            service.echec("cible@cafe.tn");
        }
        assertThat(service.estBloque("cible@cafe.tn")).isTrue();
        assertThat(service.estBloque("autre@cafe.tn")).isFalse();
    }

    @Test
    @DisplayName("la casse et les espaces ne permettent pas de contourner le comptage")
    void laCleEstNormalisee() {
        service.echec("Cible@Cafe.TN");
        service.echec("  cible@cafe.tn  ");
        service.echec("CIBLE@CAFE.TN");
        service.echec("cible@cafe.tn");
        service.echec("Cible@cafe.tn");
        service.echec("cible@CAFE.tn");

        assertThat(service.estBloque("cible@cafe.tn"))
                .as("six variantes de la meme adresse doivent compter ensemble")
                .isTrue();
    }

    @Test
    @DisplayName("une cle inconnue n'est pas bloquee")
    void uneCleInconnueEstLibre() {
        assertThat(service.estBloque("jamais-vu@cafe.tn")).isFalse();
        assertThat(service.minutesRestantes("jamais-vu@cafe.tn")).isZero();
    }
}
