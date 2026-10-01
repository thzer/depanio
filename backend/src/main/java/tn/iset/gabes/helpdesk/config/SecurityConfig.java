package tn.iset.gabes.helpdesk.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tn.iset.gabes.helpdesk.security.JwtAuthenticationFilter;

import java.util.List;

/**
 * Configuration de la securite de l'API.
 * <p>
 * L'API est sans etat : aucune session HTTP n'est creee, l'identite est
 * portee par le jeton JWT a chaque requete. La protection CSRF est donc
 * desactivee, car elle ne protege que les authentifications par cookie.
 * <p>
 * Les autorisations fines par role sont declarees directement sur les
 * methodes des controleurs avec {@code @PreAuthorize}, ce qui garde la
 * regle visible a cote du code qu'elle protege.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** Points d'entree de l'API ouverts sans authentification. */
    private static final String[] PUBLIC_API_PATHS = {
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/public/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health",
            "/actuator/health/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final HelpdeskProperties properties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthenticationEntryPoint entryPoint) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling.authenticationEntryPoint(entryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_API_PATHS).permitAll()
                        // Les requetes de pre-verification CORS ne portent pas de jeton
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Le canal temps reel est ouvert au handshake ; l'identite
                        // est etablie a la trame CONNECT par l'intercepteur STOMP
                        .requestMatchers("/ws/**", "/ws-sockjs/**").permitAll()

                        // Toutes les donnees passent par l'API : c'est elle qui
                        // est protegee, point par point.
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/actuator/**").authenticated()

                        // Le reste est l'application Angular elle-meme : son code
                        // et ses adresses de navigation. Ces fichiers sont publics
                        // par nature, puisque tout navigateur les telecharge avant
                        // meme qu'un utilisateur se soit identifie. Les proteger
                        // n'apporterait rien et empecherait simplement l'ecran de
                        // connexion de s'afficher.
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter,
                        org.springframework.security.web.authentication
                                .UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Renvoie une erreur 401 au format JSON plutot que la page de connexion
     * HTML par defaut, que le frontend ne saurait pas interpreter.
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                    {"status":401,"error":"Unauthorized",\
                    "message":"Authentification requise."}""");
        };
    }

    /**
     * Algorithme de hachage des mots de passe.
     * <p>
     * Declarer ce bean a cote du bean {@code UserDetailsService} suffit :
     * Spring Boot assemble lui-meme le fournisseur d'authentification par
     * mot de passe, il est donc inutile de le construire a la main.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt applique automatiquement un sel different par mot de passe
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Autorise les appels du frontend Angular, servi sur un autre port en
     * developpement. Les origines permises sont configurables.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.getCors().getAllowedOrigins());
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
