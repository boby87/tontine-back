package cm.ftg.tontine.service;

import cm.ftg.tontine.domain.*;
import cm.ftg.tontine.exception.*;
import cm.ftg.tontine.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContributionService")
class ContributionServiceTest {

    @Mock private MembreRepository membreRepository;
    @Mock private SeanceRepository seanceRepository;
    @Mock private CotisationRepository cotisationRepository;
    @Mock private AmendeRepository amendeRepository;
    @Mock private NotificationService notificationService;
    @Mock private ExecutorService notificationExecutor;

    @InjectMocks
    private ContributionService contributionService;

    private Tontine tontine;
    private Membre membreActif;
    private Seance seanceOuverte;

    @BeforeEach
    void setUp() {
        tontine = new Tontine();
        tontine.setId(1L);
        tontine.setNom("Tontine Solidaire");
        tontine.setMontantCotisation(new BigDecimal("5000"));
        tontine.setTauxAmendeForfaitaireJour(new BigDecimal("500"));
        tontine.setPlafondAmendeEnPourcentage(new BigDecimal("50"));

        membreActif = new Membre();
        membreActif.setId(10L);
        membreActif.setNom("Jean Dupont");
        membreActif.setStatut(StatutMembre.ACTIF);
        membreActif.setTontine(tontine);

        seanceOuverte = new Seance();
        seanceOuverte.setId(100L);
        seanceOuverte.setDateSeance(LocalDate.of(2026, 4, 5));
        seanceOuverte.setDateLimite(LocalDate.of(2026, 4, 7));
        seanceOuverte.setCloturee(false);
        seanceOuverte.setTontine(tontine);
    }

    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("recordContribution()")
    class RecordContribution {

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
        @DisplayName("Doit enregistrer une cotisation valide sans amende quand le paiement est dans les délais")
        void should_persistContribution_when_validRequestOnTime() {
            // Arrange
            var request = new ContributionRequest(10L, 1L, 100L, new BigDecimal("5000"));
            seanceOuverte.setDateLimite(LocalDate.now().plusDays(1)); // pas de retard

            when(membreRepository.findByIdAndTontineId(10L, 1L)).thenReturn(Optional.of(membreActif));
            when(seanceRepository.findById(100L)).thenReturn(Optional.of(seanceOuverte));
            when(cotisationRepository.existsByMembreIdAndSeanceId(10L, 100L)).thenReturn(false);
            when(cotisationRepository.save(any(Cotisation.class))).thenAnswer(inv -> {
                Cotisation c = inv.getArgument(0);
                c.setId(1L);
                return c;
            });

            // Act
            var result = contributionService.recordContribution(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.membreId()).isEqualTo(10L);
            assertThat(result.seanceId()).isEqualTo(100L);
            assertThat(result.montantCotisation()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(result.statut()).isEqualTo(StatutCotisation.PAYEE);
            assertThat(result.montantAmende()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.joursRetard()).isZero();
            assertThat(result.cleIdempotence()).startsWith("COT-10-100-");

            verify(cotisationRepository).save(any(Cotisation.class));
            verify(amendeRepository, never()).save(any(Amende.class));
        }

        @Test
        @DisplayName("Doit enregistrer une cotisation EN_RETARD avec amende quand la date limite est dépassée")
        void should_persistContributionWithAmende_when_dateLimiteDepassee() {
            // Arrange
            var request = new ContributionRequest(10L, 1L, 100L, new BigDecimal("5000"));
            seanceOuverte.setDateLimite(LocalDate.now().minusDays(3)); // 3 jours de retard

            when(membreRepository.findByIdAndTontineId(10L, 1L)).thenReturn(Optional.of(membreActif));
            when(seanceRepository.findById(100L)).thenReturn(Optional.of(seanceOuverte));
            when(cotisationRepository.existsByMembreIdAndSeanceId(10L, 100L)).thenReturn(false);
            when(cotisationRepository.save(any(Cotisation.class))).thenAnswer(inv -> {
                Cotisation c = inv.getArgument(0);
                c.setId(2L);
                return c;
            });
            when(amendeRepository.save(any(Amende.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            var result = contributionService.recordContribution(request);

            // Assert
            assertThat(result.statut()).isEqualTo(StatutCotisation.EN_RETARD);
            assertThat(result.joursRetard()).isEqualTo(3);
            // Amende = 500 × 3 = 1500
            assertThat(result.montantAmende()).isEqualByComparingTo(new BigDecimal("1500"));

            var amendeCaptor = ArgumentCaptor.forClass(Amende.class);
            verify(amendeRepository).save(amendeCaptor.capture());
            assertThat(amendeCaptor.getValue().getMontant()).isEqualByComparingTo(new BigDecimal("1500"));
            assertThat(amendeCaptor.getValue().getJoursRetard()).isEqualTo(3);
        }

        @Test
        @DisplayName("Doit plafonner l'amende quand le retard est très élevé")
        void should_capAmende_when_retardTresEleve() {
            // Arrange — retard 10 jours → brute = 500 × 10 = 5000, plafond = 50% × 5000 = 2500
            var request = new ContributionRequest(10L, 1L, 100L, new BigDecimal("5000"));
            seanceOuverte.setDateLimite(LocalDate.now().minusDays(10));

            when(membreRepository.findByIdAndTontineId(10L, 1L)).thenReturn(Optional.of(membreActif));
            when(seanceRepository.findById(100L)).thenReturn(Optional.of(seanceOuverte));
            when(cotisationRepository.existsByMembreIdAndSeanceId(10L, 100L)).thenReturn(false);
            when(cotisationRepository.save(any(Cotisation.class))).thenAnswer(inv -> {
                Cotisation c = inv.getArgument(0);
                c.setId(3L);
                return c;
            });
            when(amendeRepository.save(any(Amende.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            var result = contributionService.recordContribution(request);

            // Assert — amende plafonnée à 2500
            assertThat(result.montantAmende()).isEqualByComparingTo(new BigDecimal("2500.00"));
        }
    }

    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Validation : existence du membre")
    class ValidationExistenceMembre {

        @Test
        @DisplayName("Doit lever MembreIntrouvableException quand le membre n'existe pas dans la tontine")
        void should_throwMembreIntrouvable_when_membreNotFound() {
            // Arrange
            var request = new ContributionRequest(999L, 1L, 100L, new BigDecimal("5000"));
            when(membreRepository.findByIdAndTontineId(999L, 1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> contributionService.recordContribution(request))
                .isInstanceOf(MembreIntrouvableException.class)
                .hasMessageContaining("999");
        }

        @Test
        @DisplayName("Doit lever MembreInactifException quand le membre est suspendu")
        void should_throwMembreInactif_when_membreSuspendu() {
            // Arrange
            membreActif.setStatut(StatutMembre.SUSPENDU);
            var request = new ContributionRequest(10L, 1L, 100L, new BigDecimal("5000"));
            when(membreRepository.findByIdAndTontineId(10L, 1L)).thenReturn(Optional.of(membreActif));

            // Act & Assert
            assertThatThrownBy(() -> contributionService.recordContribution(request))
                .isInstanceOf(MembreInactifException.class)
                .hasMessageContaining("10");
        }
    }

    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Validation : solvabilité et double-traitement")
    class ValidationSolvabilite {

        @Test
        @DisplayName("Doit lever DoubleCotisationException quand le membre a déjà cotisé pour cette séance")
        void should_throwDoubleCotisation_when_cotisationExists() {
            // Arrange
            var request = new ContributionRequest(10L, 1L, 100L, new BigDecimal("5000"));
            when(membreRepository.findByIdAndTontineId(10L, 1L)).thenReturn(Optional.of(membreActif));
            when(seanceRepository.findById(100L)).thenReturn(Optional.of(seanceOuverte));
            when(cotisationRepository.existsByMembreIdAndSeanceId(10L, 100L)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> contributionService.recordContribution(request))
                .isInstanceOf(DoubleCotisationException.class)
                .hasMessageContaining("10")
                .hasMessageContaining("100");
        }

        @Test
        @DisplayName("Doit lever SeanceNonOuverteException quand la séance est clôturée")
        void should_throwSeanceNonOuverte_when_seanceCloturee() {
            // Arrange
            seanceOuverte.setCloturee(true);
            var request = new ContributionRequest(10L, 1L, 100L, new BigDecimal("5000"));
            when(membreRepository.findByIdAndTontineId(10L, 1L)).thenReturn(Optional.of(membreActif));
            when(seanceRepository.findById(100L)).thenReturn(Optional.of(seanceOuverte));

            // Act & Assert
            assertThatThrownBy(() -> contributionService.recordContribution(request))
                .isInstanceOf(SeanceNonOuverteException.class);
        }

        @Test
        @DisplayName("Doit lever MontantInvalideException quand le montant ne correspond pas")
        void should_throwMontantInvalide_when_montantIncorrect() {
            // Arrange
            var request = new ContributionRequest(10L, 1L, 100L, new BigDecimal("3000"));
            when(membreRepository.findByIdAndTontineId(10L, 1L)).thenReturn(Optional.of(membreActif));
            when(seanceRepository.findById(100L)).thenReturn(Optional.of(seanceOuverte));
            when(cotisationRepository.existsByMembreIdAndSeanceId(10L, 100L)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> contributionService.recordContribution(request))
                .isInstanceOf(MontantInvalideException.class)
                .hasMessageContaining("5000")
                .hasMessageContaining("3000");
        }
    }

    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("calculerAmende()")
    class CalculerAmende {

        @ParameterizedTest(name = "retard={0}j → amende={1}")
        @DisplayName("Doit calculer l'amende correctement selon les jours de retard")
        @CsvSource({
            "1,  500.00",
            "2,  1000.00",
            "5,  2500.00",
            "6,  2500.00",
            "10, 2500.00"
        })
        void should_computeCorrectAmende(long joursRetard, String amendeAttendue) {
            // Arrange
            BigDecimal montantCotisation = new BigDecimal("5000");
            BigDecimal tauxJour = new BigDecimal("500");
            BigDecimal plafond = new BigDecimal("50");

            // Act
            BigDecimal result = contributionService.calculerAmende(montantCotisation, tauxJour, plafond, joursRetard);

            // Assert
            assertThat(result).isEqualByComparingTo(new BigDecimal(amendeAttendue));
        }
    }

    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("calculerJoursRetard()")
    class CalculerJoursRetard {

        @Test
        @DisplayName("Doit retourner 0 quand la date est avant la limite")
        void should_returnZero_when_beforeDateLimite() {
            assertThat(contributionService.calculerJoursRetard(
                LocalDate.of(2026, 4, 5), LocalDate.of(2026, 4, 7)
            )).isZero();
        }

        @Test
        @DisplayName("Doit retourner 0 le jour même de la date limite")
        void should_returnZero_when_onDateLimite() {
            assertThat(contributionService.calculerJoursRetard(
                LocalDate.of(2026, 4, 7), LocalDate.of(2026, 4, 7)
            )).isZero();
        }

        @Test
        @DisplayName("Doit retourner le nombre exact de jours après la limite")
        void should_returnDays_when_afterDateLimite() {
            assertThat(contributionService.calculerJoursRetard(
                LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 7)
            )).isEqualTo(3);
        }
    }
}
