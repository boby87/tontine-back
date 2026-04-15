package cm.ftg.tontine.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration CORS globale — externalisée dans application.yaml via {@link CorsProperties}.
 *
 * <p>Compatible Virtual Threads : ce bean est stateless et ne contient
 * aucun bloc {@code synchronized} ni {@code ThreadLocal}.
 * Le filtre CORS résultant s'exécute naturellement sur un Virtual Thread
 * sans risque de <em>pinning</em> du carrier thread.</p>
 *
 * @see SecurityConfig — intègre ce bean dans la SecurityFilterChain
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    private final CorsProperties corsProperties;

    public CorsConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();

        // Origines explicites — jamais de wildcard "*" (security-rules §3.4)
        config.setAllowedOrigins(corsProperties.allowedOrigins());

        // Méthodes HTTP autorisées
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Headers autorisés (Authorization pour JWT, Content-Type pour les payloads JSON)
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        // Headers exposés au frontend (traçabilité Virtual Threads)
        config.setExposedHeaders(List.of("X-Virtual-Thread", "Location"));

        // Nécessaire pour l'envoi du header Authorization (JWT Bearer)
        config.setAllowCredentials(true);

        // Durée de cache du pré-vol OPTIONS (1 heure)
        config.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}

