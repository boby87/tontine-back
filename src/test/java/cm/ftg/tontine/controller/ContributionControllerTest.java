package cm.ftg.tontine.controller;

import cm.ftg.tontine.domain.StatutCotisation;
import cm.ftg.tontine.exception.DoubleCotisationException;
import cm.ftg.tontine.exception.MembreIntrouvableException;
import cm.ftg.tontine.exception.MontantInvalideException;
import cm.ftg.tontine.service.ContributionResult;
import cm.ftg.tontine.service.ContributionService;
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

@WebMvcTest(ContributionController.class)
@Import({ContributionMapper.class, GlobalExceptionHandler.class})
@WithMockUser
@DisplayName("ContributionController")
class ContributionControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ContributionService contributionService;
    @MockitoBean private cm.ftg.tontine.security.JwtTokenProvider jwtTokenProvider;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 4, 10, 12, 0);

    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/contributions")
    class Enregistrer {

        @Test
        @DisplayName("Doit retourner 201 avec la cotisation quand la requête est valide")
        void should_return201_when_validRequest() throws Exception {
            // Arrange
            var result = new ContributionResult(
                1L, "COT-10-100-uuid", 10L, 100L,
                new BigDecimal("5000"), StatutCotisation.PAYEE,
                BigDecimal.ZERO, 0, NOW
            );
            when(contributionService.recordContribution(any())).thenReturn(result);

            var requestBody = """
                {
                    "membreId": 10,
                    "tontineId": 1,
                    "seanceId": 100,
                    "montant": 5000
                }
                """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/contributions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.membreId").value(10))
                .andExpect(jsonPath("$.statut").value("PAYEE"))
                .andExpect(jsonPath("$.montantAmende").value(0))
                .andExpect(header().exists("X-Virtual-Thread"));
        }

        @Test
        @DisplayName("Doit retourner 201 avec amende quand cotisation en retard")
        void should_return201WithAmende_when_lateContribution() throws Exception {
            // Arrange
            var result = new ContributionResult(
                2L, "COT-10-100-uuid", 10L, 100L,
                new BigDecimal("5000"), StatutCotisation.EN_RETARD,
                new BigDecimal("1500"), 3, NOW
            );
            when(contributionService.recordContribution(any())).thenReturn(result);

            var requestBody = """
                {
                    "membreId": 10,
                    "tontineId": 1,
                    "seanceId": 100,
                    "montant": 5000
                }
                """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/contributions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_RETARD"))
                .andExpect(jsonPath("$.montantAmende").value(1500))
                .andExpect(jsonPath("$.joursRetard").value(3));
        }

        @Test
        @DisplayName("Doit retourner 400 quand le montant est absent")
        void should_return400_when_montantMissing() throws Exception {
            var requestBody = """
                {
                    "membreId": 10,
                    "tontineId": 1,
                    "seanceId": 100
                }
                """;

            mockMvc.perform(post("/api/v1/contributions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Doit retourner 404 quand le membre est introuvable")
        void should_return404_when_membreNotFound() throws Exception {
            // Arrange
            when(contributionService.recordContribution(any()))
                .thenThrow(new MembreIntrouvableException(999L, 1L));

            var requestBody = """
                {
                    "membreId": 999,
                    "tontineId": 1,
                    "seanceId": 100,
                    "montant": 5000
                }
                """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/contributions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBRE_NOT_FOUND"));
        }

        @Test
        @DisplayName("Doit retourner 409 quand double cotisation")
        void should_return409_when_duplicateContribution() throws Exception {
            // Arrange
            when(contributionService.recordContribution(any()))
                .thenThrow(new DoubleCotisationException(10L, 100L));

            var requestBody = """
                {
                    "membreId": 10,
                    "tontineId": 1,
                    "seanceId": 100,
                    "montant": 5000
                }
                """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/contributions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_CONTRIBUTION"));
        }

        @Test
        @DisplayName("Doit retourner 400 quand montant invalide")
        void should_return400_when_invalidAmount() throws Exception {
            // Arrange
            when(contributionService.recordContribution(any()))
                .thenThrow(new MontantInvalideException(new BigDecimal("5000"), new BigDecimal("3000")));

            var requestBody = """
                {
                    "membreId": 10,
                    "tontineId": 1,
                    "seanceId": 100,
                    "montant": 3000
                }
                """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/contributions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AMOUNT"));
        }
    }

    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/v1/contributions/{id}")
    class Consulter {

        @Test
        @DisplayName("Doit retourner 200 avec la cotisation quand elle existe")
        void should_return200_when_cotisationExists() throws Exception {
            // Arrange
            var result = new ContributionResult(
                1L, "COT-10-100-uuid", 10L, 100L,
                new BigDecimal("5000"), StatutCotisation.PAYEE,
                BigDecimal.ZERO, 0, NOW
            );
            when(contributionService.findById(1L)).thenReturn(result);

            // Act & Assert
            mockMvc.perform(get("/api/v1/contributions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.cleIdempotence").value("COT-10-100-uuid"))
                .andExpect(header().exists("X-Virtual-Thread"));
        }
    }
}
