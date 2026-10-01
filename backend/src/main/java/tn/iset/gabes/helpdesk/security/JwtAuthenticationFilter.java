package tn.iset.gabes.helpdesk.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre execute une fois par requete : il lit le jeton JWT present dans
 * l'en-tete Authorization et place l'utilisateur correspondant dans le
 * contexte de securite.
 * <p>
 * En l'absence de jeton ou si celui-ci est invalide, le filtre laisse
 * simplement passer la requete sans authentifier : c'est la configuration
 * de securite qui decidera ensuite de refuser ou non l'acces.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null
                && SecurityContextHolder.getContext().getAuthentication() == null
                && jwtService.isTokenValid(token)) {
            authenticate(request, token);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        try {
            // Un jeton de renouvellement ne donne pas acces aux ressources
            if (jwtService.isRefreshToken(token)) {
                return;
            }

            String email = jwtService.extractEmail(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // Un compte desactive apres l'emission du jeton est refuse
            if (!userDetails.isEnabled()) {
                log.debug("Compte desactive, authentification refusee : {}", email);
                return;
            }

            var authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (UsernameNotFoundException ex) {
            // Jeton signe correctement mais compte supprime depuis
            log.debug("Compte introuvable pour un jeton valide : {}", ex.getMessage());
        }
    }

    /**
     * Extrait le jeton de l'en-tete {@code Authorization: Bearer ...}.
     *
     * @return le jeton, ou null si l'en-tete est absent ou mal forme
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
