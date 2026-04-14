package cm.ftg.tontine.service;

import cm.ftg.tontine.domain.*;
import cm.ftg.tontine.exception.*;
import cm.ftg.tontine.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService")
class SessionServiceTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private TontineRepository tontineRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private ExecutorService notificationExecutor;

    @InjectMocks private SessionService sessionService;

    private Tontine defaultTontine;
    private User defaultUser;

    @BeforeEach
    void setUp() {
        defaultTontine = new Tontine();
        defaultTontine.setId(1L);
        defaultTontine.setNom("Tontine Test");
        defaultTontine.setMontantCotisation(new java.math.BigDecimal("10000"));
        defaultTontine.setTauxAmendeForfaitaireJour(new java.math.BigDecimal("500"));
        defaultTontine.setPlafondAmendeEnPourcentage(new java.math.BigDecimal("10"));

        defaultUser = new User();
        defaultUser.setId("user-10");
        defaultUser.setEmail("user@test.com");
        defaultUser.setPhone("+237600000001");
    }

    // ──────────────────────────────────────────────
    //  planifierSession()
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("planifierSession()")
    class PlanifierSession {

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

        private CreateSessionRequest validRequest() {
            return new CreateSessionRequest(
                1L,
                LocalDate.now().plusDays(7),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                "Salle communale",
                "Cotisation du mois",
                "user-10"
            );
        }

        @Test
        @DisplayName("Doit créer une session SCHEDULED avec le bon numéro quand la requête est valide")
        void should_createScheduledSession_when_validRequest() {
            // Arrange
            var request = validRequest();
            when(tontineRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(defaultTontine));
            when(userRepository.findById("user-10"))
                .thenReturn(Optional.of(defaultUser));
            when(sessionRepository.countByTontineId(1L)).thenReturn(3);

            var savedSession = new Session();
            savedSession.setId(50L);
            savedSession.setTontine(defaultTontine);
            savedSession.setSessionNumber(4);
            savedSession.setScheduledDate(request.scheduledDate());
            savedSession.setStartTime(request.startTime());
            savedSession.setEndTimePlanned(request.endTimePlanned());
            savedSession.setLocationAddress("Salle communale");
            savedSession.setAgenda("Cotisation du mois");
            savedSession.setStatus(SessionStatus.SCHEDULED);
            savedSession.setCreatedBy(defaultUser);
            when(sessionRepository.save(any(Session.class))).thenReturn(savedSession);

            // Act
            var result = sessionService.planifierSession(request);

            // Assert
            assertThat(result.sessionId()).isEqualTo(50L);
            assertThat(result.tontineId()).isEqualTo(1L);
            assertThat(result.sessionNumber()).isEqualTo(4);
            assertThat(result.status()).isEqualTo(SessionStatus.SCHEDULED);
            assertThat(result.locationAddress()).isEqualTo("Salle communale");

            verify(sessionRepository).save(argThat(session ->
                session.getSessionNumber() == 4
                && session.getStatus() == SessionStatus.SCHEDULED
            ));
        }

        @Test
        @DisplayName("Doit lancer DateDansLePasseException quand la date est passée")
        void should_throwException_when_dateInPast() {
            // Arrange
            var request = new CreateSessionRequest(
                1L, LocalDate.now().minusDays(1),
                null, null, null, null, "user-10"
            );

            // Act & Assert
            assertThatThrownBy(() -> sessionService.planifierSession(request))
                .isInstanceOf(DateDansLePasseException.class)
                .hasMessageContaining("passé");

            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Doit lancer TontineIntrouvableException quand la tontine n'existe pas")
        void should_throwException_when_tontineNotFound() {
            // Arrange
            var request = validRequest();
            when(tontineRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> sessionService.planifierSession(request))
                .isInstanceOf(TontineIntrouvableException.class)
                .hasMessageContaining("1");

            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Doit lancer UtilisateurIntrouvableException quand l'utilisateur n'existe pas")
        void should_throwException_when_userNotFound() {
            // Arrange
            var request = validRequest();
            when(tontineRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(defaultTontine));
            when(userRepository.findById("user-10")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> sessionService.planifierSession(request))
                .isInstanceOf(UtilisateurIntrouvableException.class)
                .hasMessageContaining("user-10");

            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Doit accepter la date d'aujourd'hui (pas dans le passé)")
        void should_accept_when_dateIsToday() {
            // Arrange
            var request = new CreateSessionRequest(
                1L, LocalDate.now(),
                null, null, null, null, "user-10"
            );
            when(tontineRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(defaultTontine));
            when(userRepository.findById("user-10"))
                .thenReturn(Optional.of(defaultUser));
            when(sessionRepository.countByTontineId(1L)).thenReturn(0);

            var savedSession = new Session();
            savedSession.setId(1L);
            savedSession.setTontine(defaultTontine);
            savedSession.setSessionNumber(1);
            savedSession.setScheduledDate(LocalDate.now());
            savedSession.setStatus(SessionStatus.SCHEDULED);
            savedSession.setCreatedBy(defaultUser);
            when(sessionRepository.save(any(Session.class))).thenReturn(savedSession);

            // Act
            var result = sessionService.planifierSession(request);

            // Assert
            assertThat(result.sessionNumber()).isEqualTo(1);
            assertThat(result.status()).isEqualTo(SessionStatus.SCHEDULED);
        }
    }

    // ──────────────────────────────────────────────
    //  ouvrirSession()
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("ouvrirSession()")
    class OuvrirSession {

        @Test
        @DisplayName("Doit passer le statut à IN_PROGRESS quand la séance est SCHEDULED")
        void should_openSession_when_statusIsScheduled() {
            // Arrange
            var session = new Session();
            session.setId(5L);
            session.setTontine(defaultTontine);
            session.setSessionNumber(1);
            session.setScheduledDate(LocalDate.now());
            session.setStatus(SessionStatus.SCHEDULED);
            session.setCreatedBy(defaultUser);
            when(sessionRepository.findById(5L)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            var result = sessionService.ouvrirSession(5L);

            // Assert
            assertThat(result.status()).isEqualTo(SessionStatus.IN_PROGRESS);
            verify(sessionRepository).save(argThat(s ->
                s.getStatus() == SessionStatus.IN_PROGRESS
            ));
        }

        @Test
        @DisplayName("Doit lancer IllegalStateException quand la séance n'est pas SCHEDULED")
        void should_throwException_when_sessionNotScheduled() {
            // Arrange
            var session = new Session();
            session.setId(5L);
            session.setTontine(defaultTontine);
            session.setStatus(SessionStatus.COMPLETED);
            when(sessionRepository.findById(5L)).thenReturn(Optional.of(session));

            // Act & Assert
            assertThatThrownBy(() -> sessionService.ouvrirSession(5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLETED");
        }

        @Test
        @DisplayName("Doit lancer SeanceNonOuverteException quand la séance n'existe pas")
        void should_throwException_when_sessionNotFound() {
            // Arrange
            when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> sessionService.ouvrirSession(99L))
                .isInstanceOf(SeanceNonOuverteException.class);
        }
    }

    // ──────────────────────────────────────────────
    //  findById()
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Doit retourner le résultat quand la session existe")
        void should_returnResult_when_sessionExists() {
            // Arrange
            var session = new Session();
            session.setId(7L);
            session.setTontine(defaultTontine);
            session.setSessionNumber(3);
            session.setScheduledDate(LocalDate.now().plusDays(5));
            session.setStatus(SessionStatus.SCHEDULED);
            when(sessionRepository.findById(7L)).thenReturn(Optional.of(session));

            // Act
            var result = sessionService.findById(7L);

            // Assert
            assertThat(result.sessionId()).isEqualTo(7L);
            assertThat(result.sessionNumber()).isEqualTo(3);
        }

        @Test
        @DisplayName("Doit lancer SeanceNonOuverteException quand la session n'existe pas")
        void should_throwException_when_sessionNotFound() {
            // Arrange
            when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> sessionService.findById(99L))
                .isInstanceOf(SeanceNonOuverteException.class);
        }
    }
}

