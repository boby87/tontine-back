package cm.ftg.tontine.secretary.dashboard.service;

import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.president.membership.enums.MembershipFileKind;
import cm.ftg.tontine.president.membership.enums.MembershipFileStatus;
import cm.ftg.tontine.president.membership.repository.MembershipFileRepository;
import cm.ftg.tontine.president.session.entity.Session;
import cm.ftg.tontine.president.session.enums.SessionStatus;
import cm.ftg.tontine.president.session.repository.SessionRepository;
import cm.ftg.tontine.secretary.agenda.enums.AgendaDraftStatus;
import cm.ftg.tontine.secretary.agenda.repository.AgendaDraftRepository;
import cm.ftg.tontine.secretary.archive.repository.ArchiveDocumentRepository;
import cm.ftg.tontine.secretary.dashboard.dto.SecretaryDashboardDto;
import cm.ftg.tontine.secretary.dashboard.dto.SecretaryDashboardDto.ActivityEntryDto;
import cm.ftg.tontine.secretary.dashboard.dto.SecretaryDashboardDto.NextSessionDto;
import cm.ftg.tontine.secretary.dashboard.dto.SecretaryDashboardDto.SecretaryKpiDto;
import cm.ftg.tontine.secretary.minutes.enums.MinutesStatus;
import cm.ftg.tontine.secretary.minutes.repository.MinutesDraftRepository;
import cm.ftg.tontine.secretary.rsvp.dto.SessionRsvpSummaryDto;
import cm.ftg.tontine.secretary.rsvp.service.SessionRsvpService;
import cm.ftg.tontine.secretary.security.SecretaryAccessChecker;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecretaryDashboardService {

    private static final List<MembershipFileStatus> PENDING_STATUSES = List.of(
            MembershipFileStatus.SUBMITTED,
            MembershipFileStatus.BUREAU_REVIEW,
            MembershipFileStatus.ASSEMBLY_VOTE_PENDING,
            MembershipFileStatus.ASSEMBLY_APPROVED,
            MembershipFileStatus.ASSEMBLY_REJECTED);

    private static final List<MinutesStatus> PENDING_MINUTES = List.of(
            MinutesStatus.DRAFT, MinutesStatus.CHANGES_REQUESTED);

    private final SecretaryAccessChecker accessChecker;
    private final SessionRepository sessionRepository;
    private final SessionRsvpService rsvpService;
    private final AgendaDraftRepository agendaDraftRepository;
    private final MinutesDraftRepository minutesDraftRepository;
    private final MembershipFileRepository membershipFileRepository;
    private final MemberRepository memberRepository;
    private final ArchiveDocumentRepository archiveDocumentRepository;

    public SecretaryDashboardService(SecretaryAccessChecker accessChecker,
                                     SessionRepository sessionRepository,
                                     SessionRsvpService rsvpService,
                                     AgendaDraftRepository agendaDraftRepository,
                                     MinutesDraftRepository minutesDraftRepository,
                                     MembershipFileRepository membershipFileRepository,
                                     MemberRepository memberRepository,
                                     ArchiveDocumentRepository archiveDocumentRepository) {
        this.accessChecker = accessChecker;
        this.sessionRepository = sessionRepository;
        this.rsvpService = rsvpService;
        this.agendaDraftRepository = agendaDraftRepository;
        this.minutesDraftRepository = minutesDraftRepository;
        this.membershipFileRepository = membershipFileRepository;
        this.memberRepository = memberRepository;
        this.archiveDocumentRepository = archiveDocumentRepository;
    }

    @Transactional(readOnly = true)
    public SecretaryDashboardDto getDashboard(UUID tontineId, UUID userId) {
        accessChecker.requireSecretary(userId, tontineId);

        Session nextSession = findNextSession(tontineId);
        NextSessionDto nextSessionDto = nextSession == null ? null : new NextSessionDto(
                nextSession.getId(),
                nextSession.getNumber(),
                nextSession.getScheduledAt(),
                nextSession.getLocation(),
                Duration.between(Instant.now(), nextSession.getScheduledAt()).toDays());

        SessionRsvpSummaryDto rsvpSummary = nextSession == null ? null
                : rsvpService.summary(nextSession.getId(), tontineId, userId);

        SecretaryKpiDto kpi = new SecretaryKpiDto(
                minutesDraftRepository.countByTontineIdAndStatusIn(tontineId, PENDING_MINUTES),
                agendaDraftRepository.countByTontineIdAndStatus(tontineId, AgendaDraftStatus.DRAFT),
                membershipFileRepository.countByTontineIdAndKindAndStatusIn(
                        tontineId, MembershipFileKind.ADHESION, PENDING_STATUSES),
                membershipFileRepository.countByTontineIdAndKindAndStatusIn(
                        tontineId, MembershipFileKind.RESIGNATION, PENDING_STATUSES),
                memberRepository.countByTontineId(tontineId),
                archiveDocumentRepository.countByTontineId(tontineId));

        // Activity log à brancher sur AuditLogRepository — laissé vide pour cette itération.
        List<ActivityEntryDto> recentActivity = List.of();

        return new SecretaryDashboardDto(nextSessionDto, rsvpSummary, kpi, recentActivity);
    }

    private Session findNextSession(UUID tontineId) {
        return sessionRepository.findByTontineIdOrderByNumberAsc(tontineId).stream()
                .filter(s -> s.getStatus() == SessionStatus.SCHEDULED)
                .filter(s -> s.getScheduledAt() != null && s.getScheduledAt().isAfter(Instant.now()))
                .findFirst()
                .orElse(null);
    }
}
