package tn.iset.gabes.helpdesk.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.iset.gabes.helpdesk.config.HelpdeskProperties;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Production et verification des jetons JWT.
 * <p>
 * Le jeton porte, en plus du sujet (adresse electronique), le role et
 * l'identifiant de l'etablissement. Le serveur n'a donc aucune session a
 * conserver : chaque requete est authentifiee a partir du seul jeton, ce
 * qui permet de deployer plusieurs instances de l'API sans etat partage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_ESTABLISHMENT = "est";
    private static final String CLAIM_TOKEN_TYPE = "typ";

    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final HelpdeskProperties properties;

    /** Jeton d'acces utilise pour appeler l'API. */
    public String generateAccessToken(UserPrincipal principal) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, principal.getId());
        claims.put(CLAIM_ROLE, principal.getRole().name());
        claims.put(CLAIM_ESTABLISHMENT, principal.getEstablishmentId());
        claims.put(CLAIM_TOKEN_TYPE, TYPE_ACCESS);
        return buildToken(principal.getUsername(), claims,
                properties.getSecurity().getJwt().getExpiration());
    }

    /** Jeton de renouvellement, de duree plus longue et sans droit d'acces direct. */
    public String generateRefreshToken(UserPrincipal principal) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, principal.getId());
        claims.put(CLAIM_TOKEN_TYPE, TYPE_REFRESH);
        return buildToken(principal.getUsername(), claims,
                properties.getSecurity().getJwt().getRefreshExpiration());
    }

    private String buildToken(String subject, Map<String, Object> claims, long validityMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMillis);
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey())
                .compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Object value = parseClaims(token).get(CLAIM_USER_ID);
        return value == null ? null : Long.valueOf(value.toString());
    }

    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(parseClaims(token).get(CLAIM_TOKEN_TYPE, String.class));
    }

    /**
     * Verifie la signature et la date d'expiration du jeton.
     *
     * @return true si le jeton est exploitable
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.debug("Jeton expire : {}", ex.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Jeton invalide : {}", ex.getMessage());
            return false;
        }
    }

    /** Duree de validite restante du jeton d'acces, en secondes. */
    public long getAccessTokenValiditySeconds() {
        return properties.getSecurity().getJwt().getExpiration() / 1000;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        byte[] keyBytes = Decoders.BASE64.decode(properties.getSecurity().getJwt().getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
