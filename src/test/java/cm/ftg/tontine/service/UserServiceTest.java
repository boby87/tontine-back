package cm.ftg.tontine.service;

import cm.ftg.tontine.domain.*;
import cm.ftg.tontine.exception.*;
import cm.ftg.tontine.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoderService passwordEncoder;
    @Mock private NotificationService notificationService;
    @Mock private ExecutorService notificationExecutor;

    @InjectMocks private UserService userService;

    // ──────────────────────────────────────────────
    //  inscrireUtilisateur()
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("inscrireUtilisateur()")
    class InscrireUtilisateur {

        @BeforeEach
        void initTransactionContext() {
            TransactionSynchronizationManager.initSynchronization();
        }

        @AfterEach
        void clearTransactionContext() {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        private CreateUserRequest validRequest() {
            return new CreateUserRequest(
                "alice@example.com",
                "+237690000000",
                "SecureP@ss1",
                "Alice",
                "Dupont",
                LocalDate.of(1990, 5, 15),
                "CENTRE",
                "CNI123456"
            );
        }

        private User savedUser() {
            var user = new User();
            user.setId("uuid-abc-123");
            user.setEmail("alice@example.com");
            user.setPhone("+237690000000");
            user.setPasswordHash("$2a$12$hashedValue");
            user.setFirstName("Alice");
            user.setLastName("Dupont");
            user.setDateOfBirth(LocalDate.of(1990, 5, 15));
            user.setRegion(Region.CENTRE);
            user.setCniNumber("CNI123456");
            return user;
        }

        @Test
        @DisplayName("Doit inscrire un utilisateur quand la requête est valide")
        void should_registerUser_when_validRequest() {
            // Arrange
            var request = validRequest();
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
            when(userRepository.existsByPhone("+237690000000")).thenReturn(false);
            when(passwordEncoder.encode("SecureP@ss1")).thenReturn("$2a$12$hashedValue");
            when(userRepository.save(any(User.class))).thenReturn(savedUser());

            // Act
            var result = userService.inscrireUtilisateur(request);

            // Assert
            assertThat(result.userId()).isEqualTo("uuid-abc-123");
            assertThat(result.email()).isEqualTo("alice@example.com");
            assertThat(result.phone()).isEqualTo("+237690000000");
            assertThat(result.firstName()).isEqualTo("Alice");
            assertThat(result.lastName()).isEqualTo("Dupont");
            assertThat(result.role()).isEqualTo(UserRole.MEMBER);
            assertThat(result.region()).isEqualTo(Region.CENTRE);
        }

        @Test
        @DisplayName("Doit hacher le mot de passe avec le PasswordEncoder")
        void should_hashPassword_when_registering() {
            // Arrange
            var request = validRequest();
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByPhone(anyString())).thenReturn(false);
            when(passwordEncoder.encode("SecureP@ss1")).thenReturn("$2a$12$hashedValue");
            when(userRepository.save(any(User.class))).thenReturn(savedUser());

            // Act
            userService.inscrireUtilisateur(request);

            // Assert
            verify(passwordEncoder).encode("SecureP@ss1");
            var captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$12$hashedValue");
        }

        @Test
        @DisplayName("Doit enregistrer une notification post-commit sur Virtual Thread")
        void should_registerPostCommitNotification_when_registered() {
            // Arrange
            var request = validRequest();
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByPhone(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hash");
            when(userRepository.save(any(User.class))).thenReturn(savedUser());

            // Act
            userService.inscrireUtilisateur(request);

            // Assert — une synchronisation post-commit est enregistrée
            assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
        }

        @Test
        @DisplayName("Doit lever EmailDejaExistantException quand l'email existe déjà")
        void should_throwException_when_emailAlreadyExists() {
            // Arrange
            var request = validRequest();
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.inscrireUtilisateur(request))
                .isInstanceOf(EmailDejaExistantException.class)
                .hasMessageContaining("alice@example.com");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Doit lever TelephoneDejaExistantException quand le téléphone existe déjà")
        void should_throwException_when_phoneAlreadyExists() {
            // Arrange
            var request = validRequest();
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
            when(userRepository.existsByPhone("+237690000000")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.inscrireUtilisateur(request))
                .isInstanceOf(TelephoneDejaExistantException.class)
                .hasMessageContaining("+237690000000");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Doit lever IllegalArgumentException quand la région est invalide")
        void should_throwException_when_regionIsInvalid() {
            // Arrange
            var request = new CreateUserRequest(
                "bob@example.com",
                "+237690000001",
                "SecureP@ss1",
                "Bob",
                "Martin",
                LocalDate.of(1985, 1, 1),
                "INVALID_REGION",
                "CNI999999"
            );
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByPhone(anyString())).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> userService.inscrireUtilisateur(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INVALID_REGION");
        }

        @Test
        @DisplayName("Ne doit jamais appeler save quand l'email est dupliqué")
        void should_notSave_when_emailDuplicated() {
            // Arrange
            var request = validRequest();
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.inscrireUtilisateur(request))
                .isInstanceOf(EmailDejaExistantException.class);

            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any());
        }
    }

    // ──────────────────────────────────────────────
    //  findById()
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Doit retourner le UserResult quand l'utilisateur existe")
        void should_returnUserResult_when_userExists() {
            // Arrange
            var user = new User();
            user.setId("uuid-abc-123");
            user.setEmail("alice@example.com");
            user.setPhone("+237690000000");
            user.setFirstName("Alice");
            user.setLastName("Dupont");
            user.setDateOfBirth(LocalDate.of(1990, 5, 15));
            user.setRegion(Region.CENTRE);
            user.setCniNumber("CNI123456");
            when(userRepository.findById("uuid-abc-123")).thenReturn(Optional.of(user));

            // Act
            var result = userService.findById("uuid-abc-123");

            // Assert
            assertThat(result.userId()).isEqualTo("uuid-abc-123");
            assertThat(result.email()).isEqualTo("alice@example.com");
            assertThat(result.firstName()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("Doit lever UtilisateurIntrouvableException quand l'utilisateur n'existe pas")
        void should_throwException_when_userNotFound() {
            // Arrange
            when(userRepository.findById("unknown-id")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.findById("unknown-id"))
                .isInstanceOf(UtilisateurIntrouvableException.class)
                .hasMessageContaining("unknown-id");
        }
    }
}
