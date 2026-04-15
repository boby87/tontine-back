package cm.ftg.tontine.security;

import cm.ftg.tontine.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Fournisseur JWT — génération, parsing et validation des tokens.
 * Conformément à security-rules.skill.md §1.3 :
 * <ul>
 *   <li>Claims obligatoires : sub (userId), roles, iat, exp</li>
 *   <li>Aucune donnée sensible dans le token</li>
 * </ul>
 *
 * <p>Compatible Virtual Threads : pas de bloc {@code synchronized},
 * {@link SecretKey} est thread-safe.</p>
 */
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = jwtProperties.expirationMs();
    }

    /**
     * Génère un access token JWT pour l'utilisateur authentifié.
     *
     * @param userId identifiant UUID de l'utilisateur (claim sub)
     * @param email  email de l'utilisateur (claim email)
     * @param role   rôle plateforme (claim roles)
     * @return token JWT signé
     */
    public String generateToken(String userId, String email, String role) {
        var now = new Date();
        var expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .claim("roles", List.of(role))
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey)
            .compact();
    }

    /**
     * Valide un token JWT (signature + expiration).
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT invalide : {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrait une {@link Authentication} Spring Security depuis le token.
     */
    public Authentication getAuthentication(String token) {
        var claims = parseClaims(token);
        var userId = claims.getSubject();

        @SuppressWarnings("unchecked")
        var roles = (List<String>) claims.get("roles", List.class);
        var authorities = roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .toList();

        return new UsernamePasswordAuthenticationToken(userId, null, authorities);
    }

    /**
     * Extrait le userId (claim sub) depuis le token.
     */
    public String getUserId(String token) {
        return parseClaims(token).getSubject();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}

