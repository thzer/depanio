package tn.iset.gabes.helpdesk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Permet au serveur de distribuer l'application Angular en plus de l'API.
 * <p>
 * En production, le frontend compile est place dans
 * {@code src/main/resources/static} et le tout est livre dans une seule
 * image : une seule adresse, un seul certificat, et aucune configuration
 * de partage de ressources entre domaines a maintenir.
 * <p>
 * La navigation d'Angular se fait cote navigateur : l'adresse
 * {@code /tickets/42} n'existe pas sur le serveur. Si l'utilisateur
 * actualise la page, le serveur doit donc renvoyer {@code index.html} et
 * laisser Angular afficher le bon ecran, au lieu de repondre 404.
 * <p>
 * Les chemins de l'API, de la documentation et de la supervision sont
 * explicitement exclus de ce renvoi.
 */
@Configuration
public class SpaForwardingConfig implements WebMvcConfigurer {

    /** Prefixes qui appartiennent au serveur et ne doivent jamais etre rediriges. */
    private static final String[] SERVER_PREFIXES = {
            "api/", "ws", "ws-sockjs/", "v3/api-docs", "swagger-ui", "actuator/",
    };

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaResourceResolver());
    }

    /**
     * Sert le fichier demande s'il existe, et {@code index.html} sinon.
     */
    private static class SpaResourceResolver extends PathResourceResolver {

        private final Resource index = new ClassPathResource("static/index.html");

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requested = location.createRelative(resourcePath);

            if (requested.exists() && requested.isReadable()) {
                return requested;
            }

            // Une adresse du serveur qui n'a pas trouve preneur reste une
            // erreur : la masquer derriere la page d'accueil rendrait tout
            // defaut de l'API incomprehensible a diagnostiquer.
            for (String prefix : SERVER_PREFIXES) {
                if (resourcePath.startsWith(prefix)) {
                    return null;
                }
            }

            // Adresse de navigation : Angular prend le relais.
            // En developpement le dossier static est vide : on renvoie null,
            // le frontend etant alors servi par son propre serveur.
            return index.exists() ? index : null;
        }
    }
}
