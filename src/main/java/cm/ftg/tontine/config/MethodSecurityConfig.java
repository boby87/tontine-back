package cm.ftg.tontine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Active la sécurité par méthode (annotations {@code @PreAuthorize}, {@code @PostAuthorize}).
 * Nécessaire pour protéger les endpoints avec des expressions SpEL
 * telles que {@code @PreAuthorize("@tontineSecurity.isMemberOf(#tontineId)")}.
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}

