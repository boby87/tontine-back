package cm.ftg.tontine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Propriétés CORS externalisées — liées au préfixe {@code app.cors} dans application.yaml.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
    List<String> allowedOrigins
) {}

