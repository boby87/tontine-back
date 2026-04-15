package cm.ftg.tontine.config;

import cm.ftg.tontine.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuration Spring Security — stateless JWT, compatible Virtual Threads.
 *
 * <h3>Compatibilité Virtual Threads (Java 21)</h3>
 * <ul>
 *   <li>Mode <strong>stateless</strong> : aucune session HTTP, donc aucun verrou
 *       de session qui pourrait <em>pin</em> un carrier thread.</li>
 *   <li>Pas de bloc {@code synchronized} dans la chaîne de filtres —
 *       le filtre CORS et la négociation de sécurité utilisent des structures
 *       thread-safe sans verrou intrinsèque.</li>
 *   <li>Le {@code SecurityContext} est propagé automatiquement pour la requête
 *       en cours. Pour les tâches asynchrones (Virtual Thread executor),
 *       utiliser {@code DelegatingSecurityContextExecutorService}
 *       (voir {@link AsyncConfig}).</li>
 * </ul>
 *
 * @see CorsConfig#corsConfigurationSource() — bean CORS injecté ici
 * @see JwtAuthenticationFilter — filtre JWT injecté avant UsernamePasswordAuthenticationFilter
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // Intégration CORS via le bean CorsConfigurationSource
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // API REST stateless — pas de CSRF nécessaire
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless : pas de session HTTP (compatible Virtual Threads — pas de verrou de session)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Règles d'autorisation
            .authorizeHttpRequests(auth -> auth
                // Endpoints publics (inscription + login)
                .requestMatchers("/api/v1/users/register", "/api/v1/auth/login").permitAll()

                // Swagger / OpenAPI — accessible en dev (désactivé en prod via profil)
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // Actuator health (monitoring)
                .requestMatchers("/actuator/health").permitAll()

                // Tout le reste requiert une authentification JWT
                .anyRequest().authenticated()
            )

            // Filtre JWT avant le filtre d'authentification par défaut
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            .build();
    }
}

