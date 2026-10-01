package tn.iset.gabes.helpdesk.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentation interactive de l'API, accessible sur
 * {@code http://localhost:8080/swagger-ui.html}.
 * <p>
 * Elle sert pendant le developpement pour tester les points d'entree sans
 * frontend, et fournit pour le rapport une description a jour de l'API,
 * generee a partir du code et non recopiee a la main.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI helpdeskOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Helpdesk - Cafes et Restaurants")
                        .version("1.0.0")
                        .description("""
                                Service de gestion des tickets d'assistance technique pour \
                                les cafes et les restaurants.

                                Pour tester un point d'entree protege : appeler d'abord \
                                POST /api/auth/login, copier le champ accessToken de la \
                                reponse, puis le renseigner via le bouton Authorize.

                                Projet de Fin d'Etudes - ISET Gabes.""")
                        .contact(new Contact().name("ISET Gabes"))
                        .license(new License().name("Usage academique")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Jeton JWT obtenu via POST /api/auth/login")));
    }
}
