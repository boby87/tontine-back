package cm.ftg.tontine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés JWT externalisées — liées au préfixe {@code app.jwt} dans application.yaml.
 * Conformément à SKILL.md §1.3 : secret ≥ 256 bits, durée ≤ 15 min.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    String secret,
    long expirationMs
) {}

