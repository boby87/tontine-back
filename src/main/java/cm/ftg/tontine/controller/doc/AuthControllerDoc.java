package cm.ftg.tontine.controller.doc;

import cm.ftg.tontine.controller.LoginResponse;
import cm.ftg.tontine.service.LoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Authentification", description = "Connexion et gestion des tokens JWT")
public interface AuthControllerDoc {

    @Operation(
        summary = "Connexion utilisateur",
        description = "Authentifie un utilisateur par email et mot de passe. "
                    + "Retourne un token JWT (durée 15 min) à utiliser dans le header Authorization: Bearer {token}.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Authentification réussie",
                content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides (email manquant, validation échouée)"),
            @ApiResponse(responseCode = "401", description = "Email ou mot de passe incorrect")
        }
    )
    ResponseEntity<LoginResponse> login(LoginRequest request);
}

