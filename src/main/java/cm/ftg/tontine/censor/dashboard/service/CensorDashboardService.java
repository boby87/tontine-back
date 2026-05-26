package cm.ftg.tontine.censor.dashboard.service;

import cm.ftg.tontine.censor.attendance.enums.AttendanceModificationStatus;
import cm.ftg.tontine.censor.attendance.repository.AttendanceModificationRequestRepository;
import cm.ftg.tontine.censor.dashboard.dto.CensorDashboardDto;
import cm.ftg.tontine.censor.dashboard.dto.CensorDashboardDto.AlertsDto;
import cm.ftg.tontine.censor.dashboard.dto.CensorDashboardDto.NextSessionDto;
import cm.ftg.tontine.censor.dashboard.dto.CensorDashboardDto.SanctionBreakdownDto;
import cm.ftg.tontine.censor.dashboard.dto.CensorDashboardDto.SanctionsPeriodDto;
import cm.ftg.tontine.censor.dashboard.dto.CensorDashboardDto.TopSanctionedDto;
import cm.ftg.tontine.censor.justification.enums.JustificationStatus;
import cm.ftg.tontine.censor.justification.repository.AbsenceJustificationRepository;
import cm.ftg.tontine.censor.security.CensorAccessChecker;
import cm.ftg.tontine.president.sanction.entity.Sanction;
import cm.ftg.tontine.president.sanction.enums.SanctionStatus;
import cm.ftg.tontine.president.sanction.repository.SanctionRepository;
import cm.ftg.tontine.president.session.entity.Session;
import cm.ftg.tontine.president.session.enums.SessionStatus;
import cm.ftg.tontine.president.session.repository.SessionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CensorDashboardService {

    private static final List<JustificationStatus> PENDING_JUSTIFICATIONS = List.of(
            JustificationStatus.PENDING_CENSOR, JustificationStatus.INFO_REQUESTED);

    private final CensorAccessChecker accessChecker;
    private final SanctionRepository sanctionRepository;
    private final AttendanceModificationRequestRepository attendanceRepository;
    private final AbsenceJustificationRepository justificationRepository;
    private final SessionRepository sessionRepository;

    public CensorDashboardService(CensorAccessChecker accessChecker,
                                  SanctionRepository sanctionRepository,
                                  AttendanceModificationRequestRepository attendanceRepository,
                                  AbsenceJustificationRepository justificationRepository,
                                  SessionRepository sessionRepository) {
        this.accessChecker = accessChecker;
        this.sanctionRepository = sanctionRepository;
        this.attendanceRepository = attendanceRepository;
        this.justificationRepository = justificationRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional(readOnly = true)
    public CensorDashboardDto getDashboard(UUID tontineId, UUID userId) {
        accessChecker.requireCensor(userId, tontineId);

        long pendingAttendance = attendanceRepository.countByTontineIdAndStatus(
                tontineId, AttendanceModificationStatus.PENDING);
        long pendingJustifications = justificationRepository.countByTontineIdAndStatusIn(
                tontineId, PENDING_JUSTIFICATIONS);
        long pendingContestations = sanctionRepository.countByTontineIdAndStatus(
                tontineId, SanctionStatus.CONTESTED);

        List<Sanction> all = sanctionRepository.findByTontineIdOrderByIssuedAtDesc(tontineId);
        long unpaidCount = all.stream()
                .filter(s -> s.getStatus() == SanctionStatus.CONFIRMED && s.isFinancial())
                .count();
        BigDecimal unpaidAmount = all.stream()
                .filter(s -> s.getStatus() == SanctionStatus.CONFIRMED && s.isFinancial())
                .map(Sanction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        AlertsDto alerts = new AlertsDto(pendingAttendance, pendingJustifications,
                pendingContestations, unpaidCount, unpaidAmount);

        long periodCount = all.size();
        BigDecimal periodAmount = all.stream()
                .map(Sanction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal collected = all.stream()
                .filter(s -> s.getStatus() == SanctionStatus.PAID)
                .map(Sanction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, long[]> typeCount = new HashMap<>();
        Map<String, BigDecimal> typeAmount = new HashMap<>();
        for (Sanction s : all) {
            String key = s.getType().name();
            typeCount.computeIfAbsent(key, k -> new long[]{0})[0]++;
            typeAmount.merge(key, s.getAmount(), BigDecimal::add);
        }
        List<SanctionBreakdownDto> breakdown = typeCount.entrySet().stream()
                .map(e -> new SanctionBreakdownDto(e.getKey(), e.getValue()[0],
                        typeAmount.getOrDefault(e.getKey(), BigDecimal.ZERO)))
                .sorted(Comparator.comparingLong(SanctionBreakdownDto::count).reversed())
                .toList();

        SanctionsPeriodDto period = new SanctionsPeriodDto(periodCount, periodAmount, collected, breakdown);

        Map<UUID, long[]> memberCount = new HashMap<>();
        Map<UUID, BigDecimal> memberAmount = new HashMap<>();
        Map<UUID, String> memberName = new HashMap<>();
        for (Sanction s : all) {
            memberCount.computeIfAbsent(s.getMemberId(), k -> new long[]{0})[0]++;
            memberAmount.merge(s.getMemberId(), s.getAmount(), BigDecimal::add);
            if (s.getMemberFullName() != null) {
                memberName.putIfAbsent(s.getMemberId(), s.getMemberFullName());
            }
        }
        List<TopSanctionedDto> topSanctioned = memberCount.entrySet().stream()
                .map(e -> new TopSanctionedDto(e.getKey(),
                        memberName.getOrDefault(e.getKey(), ""),
                        e.getValue()[0],
                        memberAmount.getOrDefault(e.getKey(), BigDecimal.ZERO)))
                .sorted(Comparator.comparingLong(TopSanctionedDto::count).reversed())
                .limit(5)
                .toList();

        Session nextSession = findNextSession(tontineId);
        NextSessionDto sessionDto = nextSession == null ? null : new NextSessionDto(
                nextSession.getId(), nextSession.getNumber(),
                nextSession.getStatus().name(), nextSession.getScheduledAt());

        return new CensorDashboardDto(alerts, period, topSanctioned, sessionDto);
    }

    private Session findNextSession(UUID tontineId) {
        return sessionRepository.findByTontineIdOrderByNumberAsc(tontineId).stream()
                .filter(s -> s.getStatus() == SessionStatus.SCHEDULED
                        || s.getStatus() == SessionStatus.IN_PROGRESS)
                .filter(s -> s.getScheduledAt() != null && s.getScheduledAt().isAfter(Instant.now().minusSeconds(86_400)))
                .findFirst()
                .orElse(null);
    }
}
