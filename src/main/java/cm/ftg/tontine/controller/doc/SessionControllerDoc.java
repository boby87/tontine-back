package cm.ftg.tontine.controller.doc;

import cm.ftg.tontine.controller.SessionResponse;
import cm.ftg.tontine.service.CreateSessionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Sessions", description = "Gestion des séances : planification, ouverture et consultation")
public interface SessionControllerDoc {

    @Operation(
        summary = "Planifier une séance",
        description = "Planifie une nouvelle séance pour une tontine. "
                    + "Vérifie que la date n'est pas dans le passé et calcule automatiquement le numéro de séance.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Séance planifiée avec succès",
                content = @Content(schema = @Schema(implementation = SessionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides (date dans le passé, validation échouée)"),
            @ApiResponse(responseCode = "404", description = "Tontine ou utilisateur introuvable")
        }
    )
    ResponseEntity<SessionResponse> planifier(CreateSessionRequest request);

    @Operation(
        summary = "Ouvrir une séance",
        description = "Passe le statut d'une séance SCHEDULED à IN_PROGRESS.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Séance ouverte",
                content = @Content(schema = @Schema(implementation = SessionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Séance introuvable"),
            @ApiResponse(responseCode = "409", description = "La séance ne peut pas être ouverte (statut incompatible)")
        }
    )
    ResponseEntity<SessionResponse> ouvrir(
        @Parameter(description = "Identifiant de la séance", required = true) Long id
    );

    @Operation(
        summary = "Consulter une séance",
        description = "Retourne le détail d'une séance par son identifiant.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Séance trouvée",
                content = @Content(schema = @Schema(implementation = SessionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Séance introuvable")
        }
    )
    ResponseEntity<SessionResponse> consulter(
        @Parameter(description = "Identifiant de la séance", required = true) Long id
    );
}
