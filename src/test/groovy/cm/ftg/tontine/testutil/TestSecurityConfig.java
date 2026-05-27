package cm.ftg.tontine.testutil;

import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Config de test minimaliste : enregistre uniquement
 * {@link AuthenticationPrincipalArgumentResolver} pour permettre
 * a {@code @AuthenticationPrincipal} de fonctionner dans les
 * @WebMvcTest sans charger toute la SecurityConfig.
 */
@TestConfiguration
public class TestSecurityConfig implements WebMvcConfigurer {

    @Bean
    public AuthenticationPrincipalArgumentResolver authenticationPrincipalArgumentResolver() {
        return new AuthenticationPrincipalArgumentResolver();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(0, authenticationPrincipalArgumentResolver());
    }
}
