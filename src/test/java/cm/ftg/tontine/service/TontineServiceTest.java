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
import java.time.LocalDateTime;
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


    // ──────────────────────────────────────────────
    //  ajouterMembreParReference()
    // ──────────────────────────────────────────────

    @Nested
    @DisplayName("ajouterMembreParReference()")
    class AjouterMembreParReference {

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

        @Test
        @DisplayName("Doit ajouter un membre EN_ATTENTE quand l'utilisateur existe et que le demandeur est PRESIDENT")
        void should_addMemberAsPending_when_validRequest() {
            // Arrange
            var tontine = new Tontine();
            tontine.setId(1L);
            tontine.setNom("Tontine A");
            when(tontineRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tontine));
            when(userRepository.findById("user-1")).thenReturn(Optional.of(defaultUser));

            var president = new TontineMember();
            president.setRole(TontineRole.PRESIDENT);
            when(tontineMemberRepository.findByTontineIdAndUserId(1L, "user-1"))
                .thenReturn(Optional.of(president));

            var targetUser = new User();
            targetUser.setId("user-2");
            targetUser.setEmail("target@test.com");
            when(userRepository.findByEmailOrPhone("target@test.com", "target@test.com"))
                .thenReturn(Optional.of(targetUser));

            when(tontineMemberRepository.existsByTontineIdAndUserId(1L, "user-2")).thenReturn(false);
            when(tontineMemberRepository.countByTontineIdAndStatus(1L, TontineMemberStatus.ACTIF)).thenReturn(1);

            when(tontineMemberRepository.save(any(TontineMember.class))).thenAnswer(invocation -> {
                var m = invocation.getArgument(0, TontineMember.class);
                m.setId("member-new");
                return m;
            });

            var request = new AddMemberByReferenceRequest(1L, "user-1", "target@test.com");

            // Act
            var result = tontineService.ajouterMembreParReference(request);

            // Assert
            assertThat(result.memberId()).isEqualTo("member-new");
            assertThat(result.status()).isEqualTo(TontineMemberStatus.EN_ATTENTE);
            assertThat(result.role()).isEqualTo(TontineRole.MEMBRE);
            assertThat(result.userId()).isEqualTo("user-2");

            verify(tontineMemberRepository).save(argThat(m ->
                m.getRole() == TontineRole.MEMBRE
                && m.getStatus() == TontineMemberStatus.EN_ATTENTE
                && m.getRotationOrder() == 2
            ));
        }

        @Test
        @DisplayName("Doit lancer MembreDejaExistantException quand l'utilisateur est déjà membre")
        void should_throwException_when_memberAlreadyExists() {
            // Arrange
            var tontine = new Tontine();
            tontine.setId(1L);
            when(tontineRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tontine));
            when(userRepository.findById("user-1")).thenReturn(Optional.of(defaultUser));

            var president = new TontineMember();
            president.setRole(TontineRole.PRESIDENT);
            when(tontineMemberRepository.findByTontineIdAndUserId(1L, "user-1"))
                .thenReturn(Optional.of(president));

            var existingUser = new User();
            existingUser.setId("user-2");
            when(userRepository.findByEmailOrPhone("+237600000001", "+237600000001"))
                .thenReturn(Optional.of(existingUser));
            when(tontineMemberRepository.existsByTontineIdAndUserId(1L, "user-2")).thenReturn(true);

            var request = new AddMemberByReferenceRequest(1L, "user-1", "+237600000001");

            // Act & Assert
            assertThatThrownBy(() -> tontineService.ajouterMembreParReference(request))
                .isInstanceOf(MembreDejaExistantException.class);
        }

        @Test
        @DisplayName("Doit lancer UtilisateurIntrouvableException quand la référence ne correspond à aucun utilisateur")
        void should_throwException_when_referenceNotFound() {
            // Arrange
            var tontine = new Tontine();
            tontine.setId(1L);
            when(tontineRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tontine));
            when(userRepository.findById("user-1")).thenReturn(Optional.of(defaultUser));

            var president = new TontineMember();
            president.setRole(TontineRole.PRESIDENT);
            when(tontineMemberRepository.findByTontineIdAndUserId(1L, "user-1"))
                .thenReturn(Optional.of(president));

            when(userRepository.findByEmailOrPhone("unknown@test.com", "unknown@test.com"))
                .thenReturn(Optional.empty());

            var request = new AddMemberByReferenceRequest(1L, "user-1", "unknown@test.com");

            // Act & Assert
            assertThatThrownBy(() -> tontineService.ajouterMembreParReference(request))
                .isInstanceOf(UtilisateurIntrouvableException.class)
                .hasMessageContaining("unknown@test.com");
        }

        @Test
        @DisplayName("Doit lancer AccesNonAutoriseException quand le demandeur n'est pas PRESIDENT")
        void should_throwException_when_requesterIsNotPresident() {
            // Arrange
            var tontine = new Tontine();
            tontine.setId(1L);
            when(tontineRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tontine));
            when(userRepository.findById("user-1")).thenReturn(Optional.of(defaultUser));

            var tresorier = new TontineMember();
            tresorier.setRole(TontineRole.TRESORIER);
            when(tontineMemberRepository.findByTontineIdAndUserId(1L, "user-1"))
                .thenReturn(Optional.of(tresorier));

            var request = new AddMemberByReferenceRequest(1L, "user-1", "target@test.com");

            // Act & Assert
            assertThatThrownBy(() -> tontineService.ajouterMembreParReference(request))
                .isInstanceOf(AccesNonAutoriseException.class)
                .hasMessageContaining("PRESIDENT");
        }
    }
}
