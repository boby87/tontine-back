package cm.ftg.tontine.controller;

import cm.ftg.tontine.domain.Region;
import cm.ftg.tontine.domain.UserRole;
import cm.ftg.tontine.exception.EmailDejaExistantException;
import cm.ftg.tontine.exception.TelephoneDejaExistantException;
import cm.ftg.tontine.exception.UtilisateurIntrouvableException;
import cm.ftg.tontine.service.UserResult;
import cm.ftg.tontine.service.UserService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({UserMapper.class, GlobalExceptionHandler.class})
@WithMockUser
@DisplayName("UserController")
class UserControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserService userService;
    @MockitoBean private cm.ftg.tontine.security.JwtTokenProvider jwtTokenProvider;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 4, 11, 10, 0);

    private UserResult defaultResult() {
        return new UserResult(
            "uuid-abc-123",
            "alice@example.com",
            "+237690000000",
            "Alice",
            "Dupont",
            LocalDate.of(1990, 5, 15),
            UserRole.MEMBER,
            Region.CENTRE,
            "CNI123456",
            NOW
        );
    }

    private static final String VALID_REQUEST_BODY = """
        {
            "email": "alice@example.com",
            "phone": "+237690000000",
            "password": "SecureP@ss1",
            "firstName": "Alice",
            "lastName": "Dupont",
            "dateOfBirth": "1990-05-15",
            "region": "CENTRE",
            "cniNumber": "CNI123456"
        }
        """;

    // ──────────────────────────────────────────────
    //  POST /api/v1/users/register
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/users/register")
    class Inscrire {

        @Test
        @DisplayName("Doit retourner 201 avec le profil quand la requête est valide")
        void should_return201_when_validRequest() throws Exception {
            // Arrange
            when(userService.inscrireUtilisateur(any())).thenReturn(defaultResult());

            // Act & Assert
            mockMvc.perform(post("/api/v1/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_REQUEST_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("uuid-abc-123"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.phone").value("+237690000000"))
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.lastName").value("Dupont"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.region").value("CENTRE"))
                .andExpect(header().exists("X-Virtual-Thread"));
        }

        @Test
        @DisplayName("Doit retourner 400 quand l'email est absent")
        void should_return400_when_emailMissing() throws Exception {
            var requestBody = """
                {
                    "phone": "+237690000000",
                    "password": "SecureP@ss1",
                    "firstName": "Alice",
                    "lastName": "Dupont",
                    "dateOfBirth": "1990-05-15",
                    "region": "CENTRE",
                    "cniNumber": "CNI123456"
                }
                """;

            mockMvc.perform(post("/api/v1/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Doit retourner 400 quand l'email est invalide")
        void should_return400_when_emailInvalid() throws Exception {
            var requestBody = """
                {
                    "email": "not-an-email",
                    "phone": "+237690000000",
                    "password": "SecureP@ss1",
                    "firstName": "Alice",
                    "lastName": "Dupont",
                    "dateOfBirth": "1990-05-15",
                    "region": "CENTRE",
                    "cniNumber": "CNI123456"
                }
                """;

            mockMvc.perform(post("/api/v1/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Doit retourner 400 quand le téléphone n'est pas au format +237")
        void should_return400_when_phoneFormatInvalid() throws Exception {
            var requestBody = """
                {
                    "email": "alice@example.com",
                    "phone": "0690000000",
                    "password": "SecureP@ss1",
                    "firstName": "Alice",
                    "lastName": "Dupont",
                    "dateOfBirth": "1990-05-15",
                    "region": "CENTRE",
                    "cniNumber": "CNI123456"
                }
                """;

            mockMvc.perform(post("/api/v1/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Doit retourner 400 quand le mot de passe est trop court")
        void should_return400_when_passwordTooShort() throws Exception {
            var requestBody = """
                {
                    "email": "alice@example.com",
                    "phone": "+237690000000",
                    "password": "short",
                    "firstName": "Alice",
                    "lastName": "Dupont",
                    "dateOfBirth": "1990-05-15",
                    "region": "CENTRE",
                    "cniNumber": "CNI123456"
                }
                """;

            mockMvc.perform(post("/api/v1/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Doit retourner 409 quand l'email existe déjà")
        void should_return409_when_emailAlreadyExists() throws Exception {
            // Arrange
            when(userService.inscrireUtilisateur(any()))
                .thenThrow(new EmailDejaExistantException("alice@example.com"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_REQUEST_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
        }

        @Test
        @DisplayName("Doit retourner 409 quand le téléphone existe déjà")
        void should_return409_when_phoneAlreadyExists() throws Exception {
            // Arrange
            when(userService.inscrireUtilisateur(any()))
                .thenThrow(new TelephoneDejaExistantException("+237690000000"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_REQUEST_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_PHONE"));
        }

        @Test
        @DisplayName("Doit retourner 400 quand la région est invalide")
        void should_return400_when_regionInvalid() throws Exception {
            // Arrange
            when(userService.inscrireUtilisateur(any()))
                .thenThrow(new IllegalArgumentException("Région invalide : 'INVALID'"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_REQUEST_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        }
    }

    // ──────────────────────────────────────────────
    //  GET /api/v1/users/{id}
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/users/{id}")
    class Consulter {

        @Test
        @WithMockUser
        @DisplayName("Doit retourner 200 avec le profil quand l'utilisateur existe")
        void should_return200_when_userExists() throws Exception {
            // Arrange
            when(userService.findById("uuid-abc-123")).thenReturn(defaultResult());

            // Act & Assert
            mockMvc.perform(get("/api/v1/users/uuid-abc-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("uuid-abc-123"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.region").value("CENTRE"))
                .andExpect(header().exists("X-Virtual-Thread"));
        }

        @Test
        @WithMockUser
        @DisplayName("Doit retourner 404 quand l'utilisateur n'existe pas")
        void should_return404_when_userNotFound() throws Exception {
            // Arrange
            when(userService.findById("unknown-id"))
                .thenThrow(new UtilisateurIntrouvableException("unknown-id"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/users/unknown-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        }
    }
}
