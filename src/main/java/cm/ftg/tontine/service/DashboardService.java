package cm.ftg.tontine.service;

import cm.ftg.tontine.domain.Cotisation;
import cm.ftg.tontine.domain.SanctionStatus;
import cm.ftg.tontine.domain.TontineMemberStatus;
import cm.ftg.tontine.exception.TontineIntrouvableException;
import cm.ftg.tontine.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Service de calcul du dashboard de synthèse d'une tontine.
 * <p>
 * Les 6 requêtes sont parallélisées via des Virtual Threads
 * ({@code Executors.newVirtualThreadPerTaskExecutor()}) pour minimiser la latence.
 * <p>
 * Conventions :
 * <ul>
 *   <li>Montants en {@link BigDecimal} exclusivement</li>
 *   <li>Les montants sont masqués dans les logs ({@code cagnotte=***})</li>
 *   <li>{@code @Transactional(readOnly = true)} pour les lectures</li>
 * </ul>
 */
@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final TontineRepository tontineRepository;
    private final CotisationRepository cotisationRepository;
    private final SessionRepository sessionRepository;
    private final TontineMemberRepository tontineMemberRepository;
    private final SanctionRepository sanctionRepository;

    public DashboardService(TontineRepository tontineRepository,
                            CotisationRepository cotisationRepository,
                            SessionRepository sessionRepository,
                            TontineMemberRepository tontineMemberRepository,
                            SanctionRepository sanctionRepository) {
        this.tontineRepository = tontineRepository;
        this.cotisationRepository = cotisationRepository;
        this.sessionRepository = sessionRepository;
        this.tontineMemberRepository = tontineMemberRepository;
        this.sanctionRepository = sanctionRepository;
    }

    /**
     * Calcule le résumé du dashboard pour une tontine donnée.
     *
     * @param tontineId identifiant de la tontine
     * @return {@link DashboardSummary} avec les indicateurs clés
     * @throws TontineIntrouvableException si la tontine n'existe pas ou est supprimée
     */
    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary(Long tontineId) {

        // ── Vérification existence tontine ──
        var tontine = tontineRepository.findByIdAndDeletedAtIsNull(tontineId)
            .orElseThrow(() -> new TontineIntrouvableException(tontineId));

        int cycleSessionsCount = tontine.getCycleSessionsCount() != null
            ? tontine.getCycleSessionsCount()
            : 0;

        // ── Parallélisation des 6 requêtes via Virtual Threads ──
        BigDecimal totalCagnotte;
        LocalDate nextSessionDate;
        int activeMembersCount;
        int pendingSanctionsCount;
        int completedSessionsCount;
        List<Cotisation> recentCotisations;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            Future<BigDecimal> futureCagnotte = executor.submit(
                () -> cotisationRepository.sumMontantByTontineId(tontineId));

            Future<List<LocalDate>> futureNextDate = executor.submit(
                () -> sessionRepository.findNextScheduledDate(tontineId, PageRequest.of(0, 1)));

            Future<Integer> futureActiveMembers = executor.submit(
                () -> tontineMemberRepository.countByTontineIdAndStatus(tontineId, TontineMemberStatus.ACTIF));

            Future<Integer> futurePendingSanctions = executor.submit(
                () -> sanctionRepository.countByMember_Tontine_IdAndStatus(tontineId, SanctionStatus.IMPAYEE));

            Future<Integer> futureCompletedSessions = executor.submit(
                () -> sessionRepository.countCompletedSessions(tontineId));

            Future<List<Cotisation>> futureRecent = executor.submit(
                () -> cotisationRepository.findTop5ByTontineIdOrderByDateOperationDesc(
                    tontineId, PageRequest.of(0, 5)));

            // ── Collecte des résultats ──
            totalCagnotte = futureCagnotte.get();
            List<LocalDate> nextDates = futureNextDate.get();
            nextSessionDate = nextDates.isEmpty() ? null : nextDates.getFirst();
            activeMembersCount = futureActiveMembers.get();
            pendingSanctionsCount = futurePendingSanctions.get();
            completedSessionsCount = futureCompletedSessions.get();
            recentCotisations = futureRecent.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interruption lors du calcul du dashboard", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Erreur lors du calcul du dashboard", e);
        }

        // ── Calcul de la progression du cycle ──
        BigDecimal cycleProgressPercent = cycleSessionsCount > 0
            ? BigDecimal.valueOf(completedSessionsCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(cycleSessionsCount), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // ── Mapping des cotisations récentes en RecentTransaction ──
        List<RecentTransaction> recentTransactions = recentCotisations.stream()
            .map(c -> new RecentTransaction(
                c.getId(),
                c.getMembre().getId(),
                c.getMembre().getNom(),
                c.getMontant(),
                c.getStatut().name(),
                c.getDateOperation()
            ))
            .toList();

        log.info("Dashboard [tontineId={}, cagnotte=***] — VirtualThread={}",
            tontineId, Thread.currentThread().isVirtual());

        return new DashboardSummary(
            tontineId,
            tontine.getNom(),
            totalCagnotte,
            nextSessionDate,
            activeMembersCount,
            pendingSanctionsCount,
            cycleSessionsCount,
            completedSessionsCount,
            cycleProgressPercent,
            "XAF",
            recentTransactions
        );
    }
}

