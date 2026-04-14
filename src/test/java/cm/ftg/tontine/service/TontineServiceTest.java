package cm.ftg.tontine.service;

import cm.ftg.tontine.domain.*;
import cm.ftg.tontine.exception.*;
import cm.ftg.tontine.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TontineService")
class TontineServiceTest {

    @Mock private TontineRepository tontineRepository;
    @Mock private UserRepository userRepository;
    @Mock private TontineMemberRepository tontineMemberRepository;
    @Mock private NotificationService notificationService;
    @Mock private ExecutorService notificationExecutor;

    @InjectMocks private TontineService tontineService;

    private User defaultUser;

    @BeforeEach
    void setUp() {
        defaultUser = new User();
        defaultUser.setId("user-1");
        defaultUser.setEmail("test@test.com");
        defaultUser.setPhone("+237600000000");
    }

    // ──────────────────────────────────────────────
    //  creerTontine()
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("creerTontine()")
    class CreerTontine {

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

        private CreateTontineRequest validRequest() {
            return new CreateTontineRequest(
                "Ma Tontine",
                "Description test",
                new BigDecimal("50000"),
                new BigDecimal("500"),
                new BigDecimal("10"),
                "MONTHLY",
                "ROTATION",
                12,
                "user-1"
            );
        }

        @Test
        @DisplayName("Doit créer une tontine avec un membre PRESIDENT quand la requête est valide")
        void should_createTontineWithPresident_when_validRequest() {
            // Arrange
            var request = validRequest();
            when(userRepository.findById("user-1")).thenReturn(Optional.of(defaultUser));
            when(tontineRepository.existsByNom("Ma Tontine")).thenReturn(false);

            var savedTontine = new Tontine();
            savedTontine.setId(10L);
            savedTontine.setNom("Ma Tontine");
            savedTontine.setDescription("Description test");
            savedTontine.setMontantCotisation(new BigDecimal("50000.00"));
            savedTontine.setContributionFrequency(ContributionFrequency.MONTHLY);
            savedTontine.setDistributionMode(DistributionMode.ROTATION);
            savedTontine.setCycleSessionsCount(12);
            when(tontineRepository.save(any(Tontine.class))).thenReturn(savedTontine);

            var savedMember = new TontineMember();
            savedMember.setId("member-100");
            savedMember.setRole(TontineRole.PRESIDENT);
            savedMember.setStatus(TontineMemberStatus.ACTIF);
            when(tontineMemberRepository.save(any(TontineMember.class))).thenReturn(savedMember);

            // Act
            var result = tontineService.creerTontine(request);

            // Assert
            assertThat(result.tontineId()).isEqualTo(10L);
            assertThat(result.nom()).isEqualTo("Ma Tontine");
            assertThat(result.montantCotisation()).isEqualByComparingTo(new BigDecimal("50000.00"));
            assertThat(result.contributionFrequency()).isEqualTo(ContributionFrequency.MONTHLY);
            assertThat(result.distributionMode()).isEqualTo(DistributionMode.ROTATION);
            assertThat(result.presidentMemberId()).isEqualTo("member-100");

            verify(tontineRepository).save(any(Tontine.class));
            verify(tontineMemberRepository).save(argThat(member ->
                member.getRole() == TontineRole.PRESIDENT
                && member.getStatus() == TontineMemberStatus.ACTIF
            ));
        }

        @Test
        @DisplayName("Doit lancer UtilisateurIntrouvableException quand l'utilisateur n'existe pas")
        void should_throwException_when_userNotFound() {
            // Arrange
            var request = validRequest();
            when(userRepository.findById("user-1")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> tontineService.creerTontine(request))
                .isInstanceOf(UtilisateurIntrouvableException.class)
                .hasMessageContaining("user-1");

            verify(tontineRepository, never()).save(any());
        }

        @Test
        @DisplayName("Doit lancer NomTontineDejaExistantException quand le nom existe déjà")
        void should_throwException_when_nameAlreadyExists() {
            // Arrange
            var request = validRequest();
            when(userRepository.findById("user-1")).thenReturn(Optional.of(defaultUser));
            when(tontineRepository.existsByNom("Ma Tontine")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> tontineService.creerTontine(request))
                .isInstanceOf(NomTontineDejaExistantException.class)
                .hasMessageContaining("Ma Tontine");

            verify(tontineRepository, never()).save(any());
        }

        @Test
        @DisplayName("Doit lancer IllegalArgumentException quand la fréquence est invalide")
        void should_throwException_when_invalidFrequency() {
            // Arrange
            var request = new CreateTontineRequest(
                "Tontine", "desc",
                new BigDecimal("50000"), new BigDecimal("500"), new BigDecimal("10"),
                "INVALID_FREQ", "ROTATION", 12, "user-1"
            );
            when(userRepository.findById("user-1")).thenReturn(Optional.of(defaultUser));
            when(tontineRepository.existsByNom("Tontine")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> tontineService.creerTontine(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INVALID_FREQ");
        }

        @Test
        @DisplayName("Doit lancer IllegalArgumentException quand le mode de distribution est invalide")
        void should_throwException_when_invalidDistributionMode() {
            // Arrange
            var request = new CreateTontineRequest(
                "Tontine", "desc",
                new BigDecimal("50000"), new BigDecimal("500"), new BigDecimal("10"),
                "MONTHLY", "INVALID_MODE", 12, "user-1"
            );
            when(userRepository.findById("user-1")).thenReturn(Optional.of(defaultUser));
            when(tontineRepository.existsByNom("Tontine")).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> tontineService.creerTontine(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INVALID_MODE");
        }

        @Test
        @DisplayName("Doit appliquer setScale(2) sur les montants BigDecimal")
        void should_applyScale2_when_savingTontine() {
            // Arrange
            var request = new CreateTontineRequest(
                "Tontine Scale", null,
                new BigDecimal("50000.5"), new BigDecimal("500.1"), new BigDecimal("10.3"),
                "MONTHLY", "ROTATION", null, "user-1"
            );
            when(userRepository.findById("user-1")).thenReturn(Optional.of(defaultUser));
            when(tontineRepository.existsByNom(any())).thenReturn(false);

            var savedTontine = new Tontine();
            savedTontine.setId(1L);
            savedTontine.setNom("Tontine Scale");
            savedTontine.setMontantCotisation(new BigDecimal("50000.50"));
            savedTontine.setContributionFrequency(ContributionFrequency.MONTHLY);
            savedTontine.setDistributionMode(DistributionMode.ROTATION);
            when(tontineRepository.save(any(Tontine.class))).thenReturn(savedTontine);

            var savedMember = new TontineMember();
            savedMember.setId("member-1");
            when(tontineMemberRepository.save(any(TontineMember.class))).thenReturn(savedMember);

            // Act
            tontineService.creerTontine(request);

            // Assert — vérifier que les montants sont passés avec scale 2
            verify(tontineRepository).save(argThat(tontine ->
                tontine.getMontantCotisation().scale() == 2
                && tontine.getTauxAmendeForfaitaireJour().scale() == 2
                && tontine.getPlafondAmendeEnPourcentage().scale() == 2
            ));
        }
    }

    // ──────────────────────────────────────────────
    //  findById()
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Doit retourner le résultat quand la tontine existe")
        void should_returnResult_when_tontineExists() {
            // Arrange
            var tontine = new Tontine();
            tontine.setId(5L);
            tontine.setNom("Ma Tontine");
            tontine.setMontantCotisation(new BigDecimal("10000"));
            tontine.setContributionFrequency(ContributionFrequency.WEEKLY);
            tontine.setDistributionMode(DistributionMode.AUCTION);
            when(tontineRepository.findByIdAndDeletedAtIsNull(5L))
                .thenReturn(Optional.of(tontine));

            // Act
            var result = tontineService.findById(5L);

            // Assert
            assertThat(result.tontineId()).isEqualTo(5L);
            assertThat(result.nom()).isEqualTo("Ma Tontine");
        }

        @Test
        @DisplayName("Doit lancer TontineIntrouvableException quand la tontine n'existe pas")
        void should_throwException_when_tontineNotFound() {
            // Arrange
            when(tontineRepository.findByIdAndDeletedAtIsNull(99L))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> tontineService.findById(99L))
                .isInstanceOf(TontineIntrouvableException.class)
                .hasMessageContaining("99");
        }
    }
}
