package cm.ftg.tontine.controller.doc;

import cm.ftg.tontine.controller.DashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Dashboard", description = "Dashboard de synthèse d'une tontine — indicateurs clés du cycle en cours")
public interface DashboardControllerDoc {

    @Operation(
        summary = "Obtenir le résumé du dashboard d'une tontine",
        description = "Retourne les indicateurs clés : cagnotte totale, prochaine séance, "
                    + "membres actifs, sanctions en attente, progression du cycle et les 5 dernières transactions.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Dashboard calculé avec succès",
                content = @Content(schema = @Schema(implementation = DashboardResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Accès refusé — l'utilisateur n'est pas membre de cette tontine"),
            @ApiResponse(responseCode = "404", description = "Tontine introuvable")
        }
    )
    ResponseEntity<DashboardResponse> getSummary(
        @Parameter(description = "Identifiant de la tontine", required = true) Long tontineId
    );
}

