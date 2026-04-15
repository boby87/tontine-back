package cm.ftg.tontine.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tontineOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("TontineApp")
                .description("API de gestion de tontines communautaires — Spring Boot 4 / Java 21")
                .version("1.0")
                .contact(new Contact()
                    .name("Équipe Tontine")
                    .email("contact@ftg.cm"))
                .license(new License()
                    .name("Propriétaire")
                    .url("https://ftg.cm/licence")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Développement"),
                new Server().url("https://api.ftg.cm").description("Production")
            ));
    }
}

