package tn.iset.gabes.helpdesk.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import tn.iset.gabes.helpdesk.websocket.WebSocketAuthInterceptor;

/**
 * Canal temps reel utilise pour pousser les notifications vers les
 * navigateurs connectes.
 * <p>
 * Le protocole STOMP est employe au-dessus de WebSocket : il apporte la
 * notion de destination, ce qui permet d'adresser un message a un seul
 * utilisateur au lieu de diffuser a tous les clients connectes.
 * <p>
 * Destinations utilisees :
 * <ul>
 *   <li>{@code /user/queue/notifications} : notifications personnelles</li>
 *   <li>{@code /topic/tickets} : evenements sur les tickets, suivis par les
 *       tableaux de bord</li>
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final HelpdeskProperties properties;
    private final WebSocketAuthInterceptor authInterceptor;

    /**
     * Branche l'authentification sur le flux entrant, afin que chaque
     * session soit rattachee a un utilisateur identifie.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Diffuseur en memoire : suffisant pour une instance unique
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        // Prefixe des destinations propres a un utilisateur
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = properties.getCors().getAllowedOrigins().toArray(String[]::new);

        // Canal principal : WebSocket natif, utilise par le frontend Angular
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins);

        // Canal de repli SockJS, pour un environnement ou le WebSocket est
        // bloque par un pare-feu ou un serveur mandataire
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns(origins)
                .withSockJS();
    }
}
