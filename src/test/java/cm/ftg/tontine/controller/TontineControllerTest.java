package cm.ftg.tontine.controller;

import cm.ftg.tontine.domain.ContributionFrequency;
import cm.ftg.tontine.domain.DistributionMode;
import cm.ftg.tontine.exception.NomTontineDejaExistantException;
import cm.ftg.tontine.exception.TontineIntrouvableException;
import cm.ftg.tontine.exception.UtilisateurIntrouvableException;
import cm.ftg.tontine.service.TontineResult;
import cm.ftg.tontine.service.TontineService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TontineController.class)
@Import({TontineMapper.class, GlobalExceptionHandler.class})
@WithMockUser
@DisplayName("TontineController")
class TontineControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private cm.ftg.tontine.security.JwtTokenProvider jwtTokenProvider;

    @MockitoBean private TontineService tontineService;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 4, 11, 10, 0);

    // ──────────────────────────────────────────────
    //  POST /api/v1/tontines
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/tontines")
    class Creer {

        @Test
        @DisplayName("Doit retourner 201 avec la tontine quand la requête est valide")
        void should_return201_when_validRequest() throws Exception {
            // Arrange
            var result = new TontineResult(
                10L, "Ma Tontine", "Description test",
                new BigDecimal("50000.00"),
                ContributionFrequency.MONTHLY, DistributionMode.ROTATION,
                12, "user-1", "member-100", NOW
            );
            when(tontineService.creerTontine(any())).thenReturn(result);

            var requestBody = """
                {
                    "nom": "Ma Tontine",
                    "description": "Description test",
                    "montantCotisation": 50000,
                    "tauxAmendeForfaitaireJour": 500,
                    "plafondAmendeEnPourcentage": 10,
                    "contributionFrequency": "MONTHLY",
                    "distributionMode": "ROTATION",
                    "cycleSessionsCount": 12,
                    "createdByUserId": "user-1"
                }
                """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/tontines")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nom").value("Ma Tontine"))
                .andExpect(jsonPath("$.montantCotisation").value(50000.00))
                .andExpect(jsonPath("$.contributionFrequency").value("MONTHLY"))
                .andExpect(jsonPath("$.distributionMode").value("ROTATION"))
                .andExpect(jsonPath("$.presidentMemberId").value("member-100"))
                .andExpect(header().exists("X-Virtual-Thread"));
        }

        @Test
        @DisplayName("Doit retourner 400 quand le nom est absent")
        void should_return400_when_nomMissing() throws Exception {
            var requestBody = """
                {
                    "montantCotisation": 50000,
                    "tauxAmendeForfaitaireJour": 500,
                    "plafondAmendeEnPourcentage": 10,
                    "contributionFrequency": "MONTHLY",
                    "distributionMode": "ROTATION",
                    "createdByUserId": "user-1"
                }
                """;

            mockMvc.perform(post("/api/v1/tontines")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Doit retourner 400 quand le montant est négatif")
        void should_return400_when_montantNegative() throws Exception {
            var requestBody = """
                {
                    "nom": "Tontine X",
                    "montantCotisation": -100,
                    "tauxAmendeForfaitaireJour": 500,
                    "plafondAmendeEnPourcentage": 10,
                    "contributionFrequency": "MONTHLY",
                    "distributionMode": "ROTATION",
                    "createdByUserId": "user-1"
                }
                """;

            mockMvc.perform(post("/api/v1/tontines")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Doit retourner 404 quand l'utilisateur n'existe pas")
        void should_return404_when_userNotFound() throws Exception {
            // Arrange
            when(tontineService.creerTontine(any()))
                .thenThrow(new UtilisateurIntrouvableException("user-999"));

            var requestBody = """
                {
                    "nom": "Tontine X",
                    "montantCotisation": 50000,
                    "tauxAmendeForfaitaireJour": 500,
                    "plafondAmendeEnPourcentage": 10,
                    "contributionFrequency": "MONTHLY",
                    "distributionMode": "ROTATION",
                    "createdByUserId": "user-999"
                }
                """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/tontines")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        }

        @Test
        @DisplayName("Doit retourner 409 quand le nom existe déjà")
        void should_return409_when_nameAlreadyExists() throws Exception {
            // Arrange
            when(tontineService.creerTontine(any()))
                .thenThrow(new NomTontineDejaExistantException("Ma Tontine"));

            var requestBody = """
                {
                    "nom": "Ma Tontine",
                    "montantCotisation": 50000,
                    "tauxAmendeForfaitaireJour": 500,
                    "plafondAmendeEnPourcentage": 10,
                    "contributionFrequency": "MONTHLY",
                    "distributionMode": "ROTATION",
                    "createdByUserId": "user-1"
                }
                """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/tontines")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_TONTINE_NAME"));
        }

        @Test
        @DisplayName("Doit retourner 400 quand la fréquence est invalide")
        void should_return400_when_invalidFrequency() throws Exception {
            // Arrange
            when(tontineService.creerTontine(any()))
                .thenThrow(new IllegalArgumentException("Fréquence de cotisation invalide"));

            var requestBody = """
                {
                    "nom": "Tontine Y",
                    "montantCotisation": 50000,
                    "tauxAmendeForfaitaireJour": 500,
                    "plafondAmendeEnPourcentage": 10,
                    "contributionFrequency": "INVALID",
                    "distributionMode": "ROTATION",
                    "createdByUserId": "user-1"
                }
                """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/tontines")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        }
    }

    // ──────────────────────────────────────────────
    //  GET /api/v1/tontines/{id}
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/tontines/{id}")
    class Consulter {

        @Test
        @DisplayName("Doit retourner 200 avec la tontine quand elle existe")
        void should_return200_when_tontineExists() throws Exception {
            // Arrange
            var result = new TontineResult(
                5L, "Tontine Solidaire", "Aide mutuelle",
                new BigDecimal("10000.00"),
                ContributionFrequency.WEEKLY, DistributionMode.AUCTION,
                24, "user-2", null, NOW
            );
            when(tontineService.findById(5L)).thenReturn(result);

            // Act & Assert
            mockMvc.perform(get("/api/v1/tontines/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.nom").value("Tontine Solidaire"))
                .andExpect(jsonPath("$.contributionFrequency").value("WEEKLY"))
                .andExpect(header().exists("X-Virtual-Thread"));
        }

        @Test
        @DisplayName("Doit retourner 404 quand la tontine n'existe pas")
        void should_return404_when_tontineNotFound() throws Exception {
            // Arrange
            when(tontineService.findById(99L))
                .thenThrow(new TontineIntrouvableException(99L));

            // Act & Assert
            mockMvc.perform(get("/api/v1/tontines/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TONTINE_NOT_FOUND"));
        }
    }
}
