package cm.ftg.tontine.president.dashboard.service;

import cm.ftg.tontine.common.enums.MemberStatus;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.president.dashboard.dto.PendingValidationSummaryDto;
import cm.ftg.tontine.president.dashboard.dto.PresidentAlertDto;
import cm.ftg.tontine.president.dashboard.dto.PresidentDashboardDto;
import cm.ftg.tontine.president.dashboard.dto.PresidentKpiDto;
import cm.ftg.tontine.president.sanction.enums.SanctionStatus;
import cm.ftg.tontine.president.sanction.repository.SanctionRepository;
import cm.ftg.tontine.president.security.PresidentAccessChecker;
import cm.ftg.tontine.president.session.entity.Session;
import cm.ftg.tontine.president.session.enums.SessionStatus;
import cm.ftg.tontine.president.session.repository.SessionRepository;
import cm.ftg.tontine.president.validation.enums.ValidationStatus;
import cm.ftg.tontine.president.validation.repository.ValidationItemRepository;
import cm.ftg.tontine.tontine.entity.Cycle;
import cm.ftg.tontine.tontine.entity.Tontine;
import cm.ftg.tontine.tontine.repository.CycleRepository;
import cm.ftg.tontine.tontine.repository.TontineRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import cm.ftg.tontine.tontine.enums.CycleStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PresidentDashboardService {

    private final PresidentAccessChecker accessChecker;
    private final TontineRepository tontineRepository;
    private final CycleRepository cycleRepository;
    private final MemberRepository memberRepository;
    private final ValidationItemRepository validationRepository;
    private final SanctionRepository sanctionRepository;
    private final SessionRepository sessionRepository;

    public PresidentDashboardService(PresidentAccessChecker accessChecker,
                                     TontineRepository tontineRepository,
                                     CycleRepository cycleRepository,
                                     MemberRepository memberRepository,
                                     ValidationItemRepository validationRepository,
                                     SanctionRepository sanctionRepository,
                                     SessionRepository sessionRepository) {
        this.accessChecker = accessChecker;
        this.tontineRepository = tontineRepository;
        this.cycleRepository = cycleRepository;
        this.memberRepository = memberRepository;
        this.validationRepository = validationRepository;
        this.sanctionRepository = sanctionRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional(readOnly = true)
    public PresidentDashboardDto getDashboard(UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);

        Tontine tontine = tontineRepository.findById(tontineId).orElseThrow();
        Cycle currentCycle = resolveCurrentCycle(tontine);

        long activeMembers = memberRepository.countByTontineIdAndStatus(tontineId, MemberStatus.ACTIVE);
        long pendingValidationsCount = validationRepository.countByTontineIdAndStatus(
                tontineId, ValidationStatus.PENDING);
        long pendingSanctionsCount = sanctionRepository.countByTontineIdAndStatus(tontineId, SanctionStatus.PENDING)
                + sanctionRepository.countByTontineIdAndStatus(tontineId, SanctionStatus.CONTESTED);
        BigDecimal pendingSanctionsAmount = sanctionRepository
                .sumAmountByTontineIdAndStatus(tontineId, SanctionStatus.PENDING)
                .add(sanctionRepository.sumAmountByTontineIdAndStatus(tontineId, SanctionStatus.CONTESTED));

        Session nextSession = findNextScheduledSession(tontineId);
        Long nextSessionInDays = null;
        Integer nextSessionNumber = null;
        if (nextSession != null && nextSession.getScheduledAt() != null) {
            nextSessionInDays = Duration.between(Instant.now(), nextSession.getScheduledAt()).toDays();
            nextSessionNumber = nextSession.getNumber();
        }

        int cycleCompletedSessions = currentCycle != null ? currentCycle.getCompletedSessions() : 0;
        int cycleTotalSessions = currentCycle != null ? currentCycle.getTotalSessions() : 0;
        BigDecimal cycleProgress = (cycleTotalSessions > 0)
                ? BigDecimal.valueOf(cycleCompletedSessions * 100L)
                        .divide(BigDecimal.valueOf(cycleTotalSessions), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        PresidentKpiDto kpi = new PresidentKpiDto(
                BigDecimal.ZERO,             // totalCashBalance — module Tresorier non implemente
                activeMembers,
                0L,                          // activeLoansCount — module Prets non implemente
                BigDecimal.ZERO,             // activeLoansAmount
                pendingSanctionsCount,
                pendingSanctionsAmount,
                nextSessionInDays,
                nextSessionNumber,
                BigDecimal.ZERO,             // contributionRate — donnees cotisations non disponibles
                pendingValidationsCount,
                cycleProgress,
                cycleCompletedSessions,
                cycleTotalSessions);

        List<PresidentAlertDto> alerts = buildAlerts(pendingValidationsCount, pendingSanctionsCount);
        List<PendingValidationSummaryDto> topValidations = validationRepository
                .findByTontineIdAndStatusOrderBySubmittedAtDesc(tontineId, ValidationStatus.PENDING).stream()
                .limit(5)
                .map(PendingValidationSummaryDto::from)
                .toList();

        return new PresidentDashboardDto(kpi, alerts, topValidations,
                List.of(), List.of(), List.of());
    }

    private Cycle resolveCurrentCycle(Tontine tontine) {
        if (tontine.getCurrentCycleId() != null) {
            return cycleRepository.findById(tontine.getCurrentCycleId()).orElse(null);
        }
        return cycleRepository.findByTontineIdOrderByNumberAsc(tontine.getId()).stream()
                .filter(c -> c.getStatus() == CycleStatus.ACTIVE)
                .findFirst()
                .orElse(null);
    }

    private Session findNextScheduledSession(UUID tontineId) {
        return sessionRepository.findByTontineIdOrderByNumberAsc(tontineId).stream()
                .filter(s -> s.getStatus() == SessionStatus.SCHEDULED)
                .filter(s -> s.getScheduledAt() != null && s.getScheduledAt().isAfter(Instant.now()))
                .findFirst()
                .orElse(null);
    }

    private List<PresidentAlertDto> buildAlerts(long pendingValidations, long pendingSanctions) {
        List<PresidentAlertDto> alerts = new ArrayList<>();
        if (pendingValidations > 0) {
            alerts.add(PresidentAlertDto.warning(UUID.randomUUID(),
                    "%d validation(s) en attente de decision".formatted(pendingValidations),
                    "/president/validations"));
        }
        if (pendingSanctions > 5) {
            alerts.add(PresidentAlertDto.critical(UUID.randomUUID(),
                    "%d sanction(s) en attente d'examen".formatted(pendingSanctions),
                    "/president/sanctions"));
        }
        return alerts;
    }
}
