package cm.ftg.tontine.controller.doc;

import cm.ftg.tontine.controller.ContributionResponse;
import cm.ftg.tontine.service.ContributionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Contributions", description = "Gestion des cotisations des membres d'une tontine")
public interface ContributionControllerDoc {

    @Operation(
        summary = "Enregistrer une cotisation",
        description = "Enregistre la cotisation d'un membre pour une séance donnée. "
                    + "Calcule automatiquement une amende si la date limite est dépassée.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Cotisation enregistrée",
                content = @Content(schema = @Schema(implementation = ContributionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides (montant incorrect, validation échouée)"),
            @ApiResponse(responseCode = "404", description = "Membre ou séance introuvable"),
            @ApiResponse(responseCode = "409", description = "Double cotisation détectée")
        }
    )
    ResponseEntity<ContributionResponse> enregistrer(ContributionRequest request);

    @Operation(
        summary = "Consulter une cotisation",
        description = "Retourne le détail d'une cotisation par son identifiant.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Cotisation trouvée",
                content = @Content(schema = @Schema(implementation = ContributionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cotisation introuvable")
        }
    )
    ResponseEntity<ContributionResponse> consulter(
        @Parameter(description = "Identifiant de la cotisation", required = true) Long id
    );
}
