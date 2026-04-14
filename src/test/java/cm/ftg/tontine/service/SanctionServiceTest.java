package cm.ftg.tontine.service;

import cm.ftg.tontine.domain.*;
import cm.ftg.tontine.exception.*;
import cm.ftg.tontine.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SanctionService")
class SanctionServiceTest {

    @Mock SanctionRepository sanctionRepository;
    @Mock TontineMemberRepository tontineMemberRepository;
    @Mock SessionRepository sessionRepository;
    @Mock NotificationService notificationService;
    @Mock ExecutorService notificationExecutor;

    @InjectMocks SanctionService sanctionService;

    // ── Données de test ──

    private static final String MEMBER_ID = "member-uuid-001";
    private static final String CENSEUR_ID = "censeur-uuid-002";
    private static final Long SESSION_ID = 10L;
    private static final Long TONTINE_ID = 1L;
    private static final BigDecimal MONTANT = BigDecimal.valueOf(5000);
    private static final SanctionType TYPE = SanctionType.RETARD;
    private static final String MOTIF = "Retard de 30 minutes";

    private TontineMember membre;
    private TontineMember censeur;
    private Session session;
    private Tontine tontine;
    private User user;

    @BeforeEach
    void setUp() {
        tontine = new Tontine();
        tontine.setId(TONTINE_ID);
        tontine.setNom("Tontine Test");

        user = new User();
        user.setId(MEMBER_ID);
        user.setFirstName("Jean");
        user.setLastName("Dupont");

        membre = new TontineMember();
        membre.setId(MEMBER_ID);
        membre.setTontine(tontine);
        membre.setUser(user);
        membre.setRole(TontineRole.MEMBRE);
        membre.setStatus(TontineMemberStatus.ACTIF);

        var censeurUser = new User();
        censeurUser.setId(CENSEUR_ID);
        censeurUser.setFirstName("Paul");
        censeurUser.setLastName("Censeur");

        censeur = new TontineMember();
        censeur.setId(CENSEUR_ID);
        censeur.setTontine(tontine);
        censeur.setUser(censeurUser);
        censeur.setRole(TontineRole.CENSEUR);
        censeur.setStatus(TontineMemberStatus.ACTIF);

        session = new Session();
        session.setId(SESSION_ID);
        session.setTontine(tontine);
        session.setStatus(SessionStatus.IN_PROGRESS);
    }

    // ──────────────────────────────────────────────
    //  applySanction()
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("applySanction()")
    class ApplySanction {

        @BeforeEach
        void initTransactionContext() {
            // Simuler le contexte transactionnel Spring pour TransactionSynchronizationManager
            if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.initSynchronization();
            }
        }

        @org.junit.jupiter.api.AfterEach
        void clearTransactionContext() {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("Doit créer la sanction quand toutes les pré-conditions sont valides")
        void should_createSanction_when_allPreconditionsValid() {
            // Arrange
            when(tontineMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(membre));
            when(tontineMemberRepository.findById(CENSEUR_ID)).thenReturn(Optional.of(censeur));
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(sanctionRepository.existsByCleIdempotence(anyString())).thenReturn(false);
            when(sanctionRepository.save(any(Sanction.class))).thenAnswer(invocation -> {
                Sanction s = invocation.getArgument(0);
                s.setId("sanction-uuid-generated");
                return s;
            });

            // Act
            var result = sanctionService.applySanction(MEMBER_ID, SESSION_ID, MONTANT, TYPE, MOTIF, CENSEUR_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.sanctionId()).isEqualTo("sanction-uuid-generated");
            assertThat(result.memberId()).isEqualTo(MEMBER_ID);
            assertThat(result.sessionId()).isEqualTo(SESSION_ID);
            assertThat(result.type()).isEqualTo(SanctionType.RETARD);
            assertThat(result.montant()).isEqualByComparingTo(BigDecimal.valueOf(5000));
            assertThat(result.motif()).isEqualTo(MOTIF);
            assertThat(result.status()).isEqualTo(SanctionStatus.IMPAYEE);
            assertThat(result.cleIdempotence()).isEqualTo("SAN-%s-%d-%s".formatted(MEMBER_ID, SESSION_ID, TYPE.name()));

            verify(sanctionRepository).save(any(Sanction.class));
        }

        @Test
        @DisplayName("Doit persister le montant BigDecimal avec scale 2")
        void should_persistBigDecimalWithScale2_when_amountProvided() {
            // Arrange
            when(tontineMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(membre));
            when(tontineMemberRepository.findById(CENSEUR_ID)).thenReturn(Optional.of(censeur));
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(sanctionRepository.existsByCleIdempotence(anyString())).thenReturn(false);
            when(sanctionRepository.save(any(Sanction.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            sanctionService.applySanction(MEMBER_ID, SESSION_ID, BigDecimal.valueOf(1234.5), TYPE, MOTIF, CENSEUR_ID);

            // Assert
            var captor = ArgumentCaptor.forClass(Sanction.class);
            verify(sanctionRepository).save(captor.capture());
            assertThat(captor.getValue().getMontant()).isEqualByComparingTo(new BigDecimal("1234.50"));
            assertThat(captor.getValue().getMontant().scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("Doit lever MontantInvalideException quand montant négatif")
        void should_throwException_when_amountIsNegative() {
            // Arrange
            var montantNegatif = BigDecimal.valueOf(-100);

            // Act & Assert
            assertThatThrownBy(() -> sanctionService.applySanction(
                    MEMBER_ID, SESSION_ID, montantNegatif, TYPE, MOTIF, CENSEUR_ID))
                .isInstanceOf(MontantInvalideException.class);
        }

        @Test
        @DisplayName("Doit lever MontantInvalideException quand montant est zéro")
        void should_throwException_when_amountIsZero() {
            // Act & Assert
            assertThatThrownBy(() -> sanctionService.applySanction(
                    MEMBER_ID, SESSION_ID, BigDecimal.ZERO, TYPE, MOTIF, CENSEUR_ID))
                .isInstanceOf(MontantInvalideException.class);
        }

        @Test
        @DisplayName("Doit lever MontantInvalideException quand montant est null")
        void should_throwException_when_amountIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> sanctionService.applySanction(
                    MEMBER_ID, SESSION_ID, null, TYPE, MOTIF, CENSEUR_ID))
                .isInstanceOf(MontantInvalideException.class);
        }

        @Test
        @DisplayName("Doit lever MembreTontineIntrouvableException quand membre introuvable")
        void should_throwException_when_memberNotFound() {
            // Arrange
            when(tontineMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> sanctionService.applySanction(
                    MEMBER_ID, SESSION_ID, MONTANT, TYPE, MOTIF, CENSEUR_ID))
                .isInstanceOf(MembreTontineIntrouvableException.class)
                .hasMessageContaining(MEMBER_ID);
        }

        @Test
        @DisplayName("Doit lever MembreInactifException quand membre non actif")
        void should_throwException_when_memberNotActive() {
            // Arrange
            membre.setStatus(TontineMemberStatus.EXCLU);
            when(tontineMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(membre));

            // Act & Assert
            assertThatThrownBy(() -> sanctionService.applySanction(
                    MEMBER_ID, SESSION_ID, MONTANT, TYPE, MOTIF, CENSEUR_ID))
                .isInstanceOf(MembreInactifException.class)
                .hasMessageContaining(MEMBER_ID);
        }

        @Test
        @DisplayName("Doit lever SessionNonEnCoursException quand session non IN_PROGRESS")
        void should_throwException_when_sessionNotInProgress() {
            // Arrange
            session.setStatus(SessionStatus.SCHEDULED);
            when(tontineMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(membre));
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            // Act & Assert
            assertThatThrownBy(() -> sanctionService.applySanction(
                    MEMBER_ID, SESSION_ID, MONTANT, TYPE, MOTIF, CENSEUR_ID))
                .isInstanceOf(SessionNonEnCoursException.class)
                .hasMessageContaining(String.valueOf(SESSION_ID));
        }

        @Test
        @DisplayName("Doit lever SessionNonEnCoursException quand session introuvable")
        void should_throwException_when_sessionNotFound() {
            // Arrange
            when(tontineMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(membre));
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> sanctionService.applySanction(
                    MEMBER_ID, SESSION_ID, MONTANT, TYPE, MOTIF, CENSEUR_ID))
                .isInstanceOf(SessionNonEnCoursException.class);
        }

        @Test
        @DisplayName("Doit lever DoubleSanctionException quand clé d'idempotence existe déjà")
        void should_throwException_when_duplicateSanction() {
            // Arrange
            when(tontineMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(membre));
            when(tontineMemberRepository.findById(CENSEUR_ID)).thenReturn(Optional.of(censeur));
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(sanctionRepository.existsByCleIdempotence(anyString())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> sanctionService.applySanction(
                    MEMBER_ID, SESSION_ID, MONTANT, TYPE, MOTIF, CENSEUR_ID))
                .isInstanceOf(DoubleSanctionException.class);
        }

        @Test
        @DisplayName("Doit lever IllegalStateException quand rôle n'est ni CENSEUR ni PRESIDENT")
        void should_throwException_when_notCenseurRole() {
            // Arrange
            censeur.setRole(TontineRole.MEMBRE);
            when(tontineMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(membre));
            when(tontineMemberRepository.findById(CENSEUR_ID)).thenReturn(Optional.of(censeur));
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            // Act & Assert
            assertThatThrownBy(() -> sanctionService.applySanction(
                    MEMBER_ID, SESSION_ID, MONTANT, TYPE, MOTIF, CENSEUR_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CENSEUR");
        }

        @Test
        @DisplayName("Doit autoriser un PRESIDENT à appliquer une sanction")
        void should_createSanction_when_presidentApplies() {
            // Arrange
            censeur.setRole(TontineRole.PRESIDENT);
            when(tontineMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(membre));
            when(tontineMemberRepository.findById(CENSEUR_ID)).thenReturn(Optional.of(censeur));
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(sanctionRepository.existsByCleIdempotence(anyString())).thenReturn(false);
            when(sanctionRepository.save(any(Sanction.class))).thenAnswer(invocation -> {
                Sanction s = invocation.getArgument(0);
                s.setId("sanction-uuid-president");
                return s;
            });

            // Act
            var result = sanctionService.applySanction(MEMBER_ID, SESSION_ID, MONTANT, TYPE, MOTIF, CENSEUR_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.sanctionId()).isEqualTo("sanction-uuid-president");
            verify(sanctionRepository).save(any(Sanction.class));
        }

        @Test
        @DisplayName("Doit enregistrer la notification post-commit sur Virtual Thread")
        void should_registerPostCommitNotification_when_sanctionCreated() {
            // Arrange
            when(tontineMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(membre));
            when(tontineMemberRepository.findById(CENSEUR_ID)).thenReturn(Optional.of(censeur));
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(sanctionRepository.existsByCleIdempotence(anyString())).thenReturn(false);
            when(sanctionRepository.save(any(Sanction.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            sanctionService.applySanction(MEMBER_ID, SESSION_ID, MONTANT, TYPE, MOTIF, CENSEUR_ID);

            // Assert — une synchronisation post-commit a été enregistrée
            var synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).isNotEmpty();

            // Simuler le commit pour vérifier que la notification est soumise
            synchronizations.forEach(TransactionSynchronization::afterCommit);
            verify(notificationExecutor).submit(any(Runnable.class));
        }

        @Test
        @DisplayName("Doit générer la bonne clé d'idempotence SAN-{memberId}-{sessionId}-{type}")
        void should_generateCorrectIdempotencyKey() {
            // Arrange
            when(tontineMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(membre));
            when(tontineMemberRepository.findById(CENSEUR_ID)).thenReturn(Optional.of(censeur));
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(sanctionRepository.existsByCleIdempotence(anyString())).thenReturn(false);
            when(sanctionRepository.save(any(Sanction.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            var result = sanctionService.applySanction(
                MEMBER_ID, SESSION_ID, MONTANT, SanctionType.ABSENCE_NON_JUSTIFIEE, MOTIF, CENSEUR_ID);

            // Assert
            assertThat(result.cleIdempotence()).isEqualTo(
                "SAN-%s-%d-ABSENCE_NON_JUSTIFIEE".formatted(MEMBER_ID, SESSION_ID));
        }

        @Test
        @DisplayName("Doit lever IllegalStateException quand censeur d'une autre tontine")
        void should_throwException_when_censeurFromDifferentTontine() {
            // Arrange
            var autreTontine = new Tontine();
            autreTontine.setId(999L);
            censeur.setTontine(autreTontine);

            when(tontineMemberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(membre));
            when(tontineMemberRepository.findById(CENSEUR_ID)).thenReturn(Optional.of(censeur));
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            // Act & Assert
            assertThatThrownBy(() -> sanctionService.applySanction(
                    MEMBER_ID, SESSION_ID, MONTANT, TYPE, MOTIF, CENSEUR_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("même tontine");
        }
    }
}
