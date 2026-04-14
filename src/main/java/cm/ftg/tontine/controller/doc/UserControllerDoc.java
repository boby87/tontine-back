package cm.ftg.tontine.controller.doc;

import cm.ftg.tontine.controller.UserResponse;
import cm.ftg.tontine.service.CreateUserRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Utilisateurs", description = "Inscription et consultation du profil utilisateur")
public interface UserControllerDoc {

    @Operation(
        summary = "Inscription d'un utilisateur",
        description = "Crée un nouveau compte utilisateur. "
                    + "Vérifie l'unicité de l'email et du téléphone, hache le mot de passe (BCrypt).",
        responses = {
            @ApiResponse(responseCode = "201", description = "Utilisateur inscrit avec succès",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides (validation échouée, région inconnue)"),
            @ApiResponse(responseCode = "409", description = "Email ou téléphone déjà utilisé")
        }
    )
    ResponseEntity<UserResponse> inscrire(CreateUserRequest request);

    @Operation(
        summary = "Consulter le profil d'un utilisateur",
        description = "Retourne le profil d'un utilisateur par son identifiant UUID.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Utilisateur trouvé",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
        }
    )
    ResponseEntity<UserResponse> consulter(
        @Parameter(description = "Identifiant UUID de l'utilisateur", required = true) String id
    );
}
