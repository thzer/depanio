package tn.iset.gabes.helpdesk.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import tn.iset.gabes.helpdesk.security.CustomUserDetailsService;
import tn.iset.gabes.helpdesk.security.JwtService;

/**
 * Authentifie la connexion au canal temps reel.
 * <p>
 * Une connexion WebSocket ne traverse pas la chaine de filtres HTTP : le
 * filtre JWT habituel n'intervient donc qu'au moment du handshake, pas sur
 * les trames STOMP. L'identite est ici etablie a la reception de la trame
 * CONNECT, qui porte le meme en-tete {@code Authorization} que les appels
 * REST.
 * <p>
 * Sans cette etape, le serveur ne saurait pas a qui appartient la session
 * et les notifications personnelles, adressees a
 * {@code /user/queue/notifications}, n'atteindraient personne.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String token = extractToken(accessor);
        if (token == null || !jwtService.isTokenValid(token)) {
            log.debug("Connexion au canal temps reel sans jeton valide : session anonyme");
            return message;
        }

        try {
            String email = jwtService.extractEmail(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (!userDetails.isEnabled()) {
                return message;
            }

            // Le nom de l'utilisateur ainsi associe a la session est celui
            // qui sera resolu par convertAndSendToUser cote serveur
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()));

            log.debug("Canal temps reel ouvert pour {}", email);

        } catch (Exception ex) {
            log.warn("Authentification du canal temps reel impossible : {}", ex.getMessage());
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
