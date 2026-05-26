package cm.ftg.tontine.security;

import cm.ftg.tontine.auth.entity.UserEntity;
import cm.ftg.tontine.common.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public enum TokenType { ACCESS, REFRESH }

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] secretBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "Le secret JWT doit faire au minimum 256 bits (32 octets). Configurer app.security.jwt.secret.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    public String generateAccessToken(UserEntity user) {
        return generate(user, TokenType.ACCESS, properties.accessTokenTtlSeconds());
    }

    public String generateRefreshToken(UserEntity user) {
        return generate(user, TokenType.REFRESH, properties.refreshTokenTtlSeconds());
    }

    public long accessTokenTtlSeconds() {
        return properties.accessTokenTtlSeconds();
    }

    private String generate(UserEntity user, TokenType type, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plusSeconds(ttlSeconds)))
                .claim("type", type.name())
                .claim("email", user.getEmail())
                .claim("phone", user.getPhone())
                .claim("roles", user.getRoles().stream().map(Enum::name).toList())
                .claim("activeTontineId",
                        user.getActiveTontineId() != null ? user.getActiveTontineId().toString() : null)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public ParsedToken parse(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token);
            Claims c = jws.getPayload();
            UUID userId = UUID.fromString(c.getSubject());
            TokenType type = TokenType.valueOf(c.get("type", String.class));
            @SuppressWarnings("unchecked")
            List<String> rolesRaw = c.get("roles", List.class);
            List<UserRole> roles = rolesRaw == null
                    ? List.of()
                    : rolesRaw.stream().map(UserRole::valueOf).toList();
            String activeTontineId = c.get("activeTontineId", String.class);
            return new ParsedToken(userId, type, roles, activeTontineId, c.getExpiration().toInstant());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidJwtException("Token invalide ou expire", ex);
        }
    }

    public record ParsedToken(UUID userId, TokenType type, List<UserRole> roles,
                              String activeTontineId, Instant expiresAt) {
    }

    public static class InvalidJwtException extends RuntimeException {
        public InvalidJwtException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
