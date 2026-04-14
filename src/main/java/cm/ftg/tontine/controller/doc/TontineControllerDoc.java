package cm.ftg.tontine.controller.doc;

import cm.ftg.tontine.controller.TontineResponse;
import cm.ftg.tontine.service.CreateTontineRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Tontines", description = "Gestion des tontines : création, configuration et consultation")
public interface TontineControllerDoc {

    @Operation(
        summary = "Créer une tontine",
        description = "Crée une nouvelle tontine et désigne le créateur comme PRESIDENT. "
                    + "Vérifie l'unicité du nom et la validité des paramètres financiers.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Tontine créée avec succès",
                content = @Content(schema = @Schema(implementation = TontineResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides (validation échouée, fréquence ou mode inconnu)"),
            @ApiResponse(responseCode = "404", description = "Utilisateur créateur introuvable"),
            @ApiResponse(responseCode = "409", description = "Nom de tontine déjà existant")
        }
    )
    ResponseEntity<TontineResponse> creer(CreateTontineRequest request);

    @Operation(
        summary = "Consulter une tontine",
        description = "Retourne le détail d'une tontine par son identifiant.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Tontine trouvée",
                content = @Content(schema = @Schema(implementation = TontineResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tontine introuvable")
        }
    )
    ResponseEntity<TontineResponse> consulter(
        @Parameter(description = "Identifiant de la tontine", required = true) Long id
    );
}
