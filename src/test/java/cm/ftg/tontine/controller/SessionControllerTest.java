package cm.ftg.tontine.controller;

import cm.ftg.tontine.domain.SessionStatus;
import cm.ftg.tontine.exception.DateDansLePasseException;
import cm.ftg.tontine.exception.SeanceNonOuverteException;
import cm.ftg.tontine.exception.TontineIntrouvableException;
import cm.ftg.tontine.exception.UtilisateurIntrouvableException;
import cm.ftg.tontine.service.SessionResult;
import cm.ftg.tontine.service.SessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionController.class)
@Import({SessionMapper.class, GlobalExceptionHandler.class})
@DisplayName("SessionController")
class SessionControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SessionService sessionService;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 4, 11, 10, 0);

    // ──────────────────────────────────────────────
    //  POST /api/v1/sessions
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/sessions")
    class Planifier {

        @Test
        @DisplayName("Doit retourner 201 avec la session quand la requête est valide")
        void should_return201_when_validRequest() throws Exception {
            // Arrange
            var result = new SessionResult(
                1L, 10L, 4,
                LocalDate.of(2026, 4, 20),
                LocalTime.of(14, 0), LocalTime.of(16, 0),
                "Salle communale", "Cotisation mensuelle",
                SessionStatus.SCHEDULED, "user-5", NOW
            );
            when(sessionService.planifierSession(any())).thenReturn(result);

            var requestBody = """
                {
                    "tontineId": 10,
                    "scheduledDate": "2026-04-20",
                    "startTime": "14:00",
                    "endTimePlanned": "16:00",
                    "locationAddress": "Salle communale",
                    "agenda": "Cotisation mensuelle",
                    "createdByUserId": "user-5"
                }
                """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.tontineId").value(10))
                .andExpect(jsonPath("$.sessionNumber").value(4))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.locationAddress").value("Salle communale"))
                .andExpect(header().exists("X-Virtual-Thread"));
        }

        @Test
        @DisplayName("Doit retourner 400 quand la date est absente")
        void should_return400_when_dateMissing() throws Exception {
            var requestBody = """
                {
                    "tontineId": 10,
                    "createdByUserId": "user-5"
                }
                """;

            mockMvc.perform(post("/api/v1/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Doit retourner 400 quand la date est dans le passé")
        void should_return400_when_dateInPast() throws Exception {
            // Arrange
            when(sessionService.planifierSession(any()))
                .thenThrow(new DateDansLePasseException(LocalDate.of(2020, 1, 1)));

            var requestBody = """
                {
                    "tontineId": 10,
                    "scheduledDate": "2020-01-01",
                    "createdByUserId": "user-5"
                }
                """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DATE_IN_PAST"));
        }

        @Test
        @DisplayName("Doit retourner 404 quand la tontine n'existe pas")
        void should_return404_when_tontineNotFound() throws Exception {
            // Arrange
            when(sessionService.planifierSession(any()))
                .thenThrow(new TontineIntrouvableException(999L));

            var requestBody = """
                {
                    "tontineId": 999,
                    "scheduledDate": "2026-05-01",
                    "createdByUserId": "user-5"
                }
                """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TONTINE_NOT_FOUND"));
        }

        @Test
        @DisplayName("Doit retourner 404 quand l'utilisateur n'existe pas")
        void should_return404_when_userNotFound() throws Exception {
            // Arrange
            when(sessionService.planifierSession(any()))
                .thenThrow(new UtilisateurIntrouvableException("user-999"));

            var requestBody = """
                {
                    "tontineId": 10,
                    "scheduledDate": "2026-05-01",
                    "createdByUserId": "user-999"
                }
                """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        }
    }

    // ──────────────────────────────────────────────
    //  PUT /api/v1/sessions/{id}/ouvrir
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/v1/sessions/{id}/ouvrir")
    class Ouvrir {

        @Test
        @DisplayName("Doit retourner 200 quand la séance passe à IN_PROGRESS")
        void should_return200_when_sessionOpened() throws Exception {
            // Arrange
            var result = new SessionResult(
                5L, 10L, 2,
                LocalDate.of(2026, 4, 11),
                LocalTime.of(14, 0), LocalTime.of(16, 0),
                "Bureau", "Ordre du jour",
                SessionStatus.IN_PROGRESS, "user-3", NOW
            );
            when(sessionService.ouvrirSession(5L)).thenReturn(result);

            // Act & Assert
            mockMvc.perform(put("/api/v1/sessions/5/ouvrir"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(header().exists("X-Virtual-Thread"));
        }

        @Test
        @DisplayName("Doit retourner 409 quand la séance n'est pas SCHEDULED")
        void should_return409_when_sessionNotScheduled() throws Exception {
            // Arrange
            when(sessionService.ouvrirSession(5L))
                .thenThrow(new IllegalStateException("La séance 5 ne peut pas être ouverte (statut actuel : COMPLETED)"));

            // Act & Assert
            mockMvc.perform(put("/api/v1/sessions/5/ouvrir"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));
        }

        @Test
        @DisplayName("Doit retourner 404 quand la séance n'existe pas")
        void should_return404_when_sessionNotFound() throws Exception {
            // Arrange
            when(sessionService.ouvrirSession(99L))
                .thenThrow(new SeanceNonOuverteException(99L));

            // Act & Assert
            mockMvc.perform(put("/api/v1/sessions/99/ouvrir"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SEANCE_NOT_FOUND"));
        }
    }

    // ──────────────────────────────────────────────
    //  GET /api/v1/sessions/{id}
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/sessions/{id}")
    class Consulter {

        @Test
        @DisplayName("Doit retourner 200 avec la session quand elle existe")
        void should_return200_when_sessionExists() throws Exception {
            // Arrange
            var result = new SessionResult(
                7L, 10L, 3,
                LocalDate.of(2026, 5, 1),
                LocalTime.of(10, 0), LocalTime.of(12, 0),
                "Centre", "Bilan",
                SessionStatus.SCHEDULED, "user-2", NOW
            );
            when(sessionService.findById(7L)).thenReturn(result);

            // Act & Assert
            mockMvc.perform(get("/api/v1/sessions/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.tontineId").value(10))
                .andExpect(jsonPath("$.sessionNumber").value(3))
                .andExpect(header().exists("X-Virtual-Thread"));
        }

        @Test
        @DisplayName("Doit retourner 404 quand la session n'existe pas")
        void should_return404_when_sessionNotFound() throws Exception {
            // Arrange
            when(sessionService.findById(99L))
                .thenThrow(new SeanceNonOuverteException(99L));

            // Act & Assert
            mockMvc.perform(get("/api/v1/sessions/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SEANCE_NOT_FOUND"));
        }
    }
}
