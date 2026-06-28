package cm.ftg.tontine.member.service;

import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.dto.MemberAttendanceEntry;
import cm.ftg.tontine.member.dto.MemberContributionEntry;
import cm.ftg.tontine.member.dto.MemberSessionView;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.president.session.entity.Session;
import cm.ftg.tontine.president.session.repository.SessionAttendanceRepository;
import cm.ftg.tontine.president.session.repository.SessionRepository;
import cm.ftg.tontine.secretary.agenda.dto.AgendaDraftItemDto;
import cm.ftg.tontine.secretary.agenda.entity.AgendaDraft;
import cm.ftg.tontine.secretary.agenda.enums.AgendaDraftStatus;
import cm.ftg.tontine.secretary.agenda.repository.AgendaDraftItemRepository;
import cm.ftg.tontine.secretary.agenda.repository.AgendaDraftRepository;
import cm.ftg.tontine.secretary.session.dto.SecretaryCycleDto;
import cm.ftg.tontine.tontine.entity.Cycle;
import cm.ftg.tontine.tontine.repository.CycleRepository;
import cm.ftg.tontine.treasurer.contribution.repository.ContributionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberSessionService {

    private static final Set<AgendaDraftStatus> VISIBLE_STATUSES =
            Set.of(AgendaDraftStatus.APPROVED, AgendaDraftStatus.PUBLISHED);

    private final MemberService memberService;
    private final CycleRepository cycleRepository;
    private final SessionRepository sessionRepository;
    private final SessionAttendanceRepository attendanceRepository;
    private final ContributionRepository contributionRepository;
    private final MemberRepository memberRepository;
    private final AgendaDraftRepository agendaDraftRepository;
    private final AgendaDraftItemRepository agendaDraftItemRepository;

    public MemberSessionService(
            MemberService memberService,
            CycleRepository cycleRepository,
            SessionRepository sessionRepository,
            SessionAttendanceRepository attendanceRepository,
            ContributionRepository contributionRepository,
            MemberRepository memberRepository,
            AgendaDraftRepository agendaDraftRepository,
            AgendaDraftItemRepository agendaDraftItemRepository) {
        this.memberService = memberService;
        this.cycleRepository = cycleRepository;
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.contributionRepository = contributionRepository;
        this.memberRepository = memberRepository;
        this.agendaDraftRepository = agendaDraftRepository;
        this.agendaDraftItemRepository = agendaDraftItemRepository;
    }

    @Transactional(readOnly = true)
    public List<SecretaryCycleDto> listCycles(UUID userId, UUID tontineId) {
        UUID effectiveTontineId = memberService.resolveActiveMember(userId, tontineId).getTontineId();
        return cycleRepository.findByTontineIdOrderByNumberAsc(effectiveTontineId)
                .stream()
                .map(SecretaryCycleDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MemberSessionView> listSessionsByCycle(UUID cycleId, UUID userId, UUID tontineId) {
        UUID effectiveTontineId = memberService.resolveActiveMember(userId, tontineId).getTontineId();
        Cycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle", cycleId));
        if (!cycle.getTontineId().equals(effectiveTontineId)) {
            throw new ApiException("FORBIDDEN", "Cycle hors tontine", HttpStatus.FORBIDDEN);
        }
        return sessionRepository.findByTontineIdAndCycleIdOrderByNumberAsc(effectiveTontineId, cycleId)
                .stream()
                .map(s -> toView(s, cycle, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MemberSessionView> listSessionsWithApprovedAgenda(UUID userId, UUID tontineId) {
        UUID effectiveTontineId = memberService.resolveActiveMember(userId, tontineId).getTontineId();
        Map<UUID, Cycle> cycleMap = cycleRepository.findByTontineIdOrderByNumberAsc(effectiveTontineId)
                .stream()
                .collect(Collectors.toMap(Cycle::getId, c -> c));
        return sessionRepository.findByTontineIdOrderByNumberAsc(effectiveTontineId).stream()
                .map(s -> toView(s, cycleMap.get(s.getCycleId()), false))
                .filter(v -> v.agendaApprovedAt() != null)
                .toList();
    }

    @Transactional(readOnly = true)
    public MemberSessionView getSessionDetail(UUID sessionId, UUID userId, UUID tontineId) {
        UUID effectiveTontineId = memberService.resolveActiveMember(userId, tontineId).getTontineId();
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));
        if (!session.getTontineId().equals(effectiveTontineId)) {
            throw new ApiException("FORBIDDEN", "Session hors tontine", HttpStatus.FORBIDDEN);
        }
        Cycle cycle = cycleRepository.findById(session.getCycleId())
                .orElseThrow(() -> new ResourceNotFoundException("Cycle", session.getCycleId()));
        return toView(session, cycle, true);
    }

    private MemberSessionView toView(Session s, Cycle cycle, boolean withDetails) {
        Optional<AgendaDraft> draft = agendaDraftRepository
                .findFirstByTontineIdAndSessionIdAndStatusInOrderByApprovedAtDesc(
                        s.getTontineId(), s.getId(), VISIBLE_STATUSES);

        List<AgendaDraftItemDto> agenda = withDetails
                ? draft.map(d -> agendaDraftItemRepository.findByAgendaDraftIdOrderByOrderIdxAsc(d.getId())
                        .stream().map(AgendaDraftItemDto::from).toList())
                        .orElse(List.of())
                : List.of();

        List<MemberAttendanceEntry> attendance = List.of();
        List<MemberContributionEntry> contributions = List.of();

        if (withDetails) {
            attendance = attendanceRepository.findBySessionIdOrderByFullNameAsc(s.getId())
                    .stream()
                    .map(MemberAttendanceEntry::from)
                    .toList();

            Map<UUID, String> memberNames = memberRepository.findByTontineId(s.getTontineId())
                    .stream()
                    .collect(Collectors.toMap(
                            m -> m.getId(),
                            m -> m.getFirstName() + " " + m.getLastName()));

            contributions = contributionRepository
                    .findByTontineIdAndSessionIdOrderByMemberIdAsc(s.getTontineId(), s.getId())
                    .stream()
                    .map(c -> MemberContributionEntry.from(c, memberNames.getOrDefault(c.getMemberId(), "?")))
                    .toList();
        }

        return new MemberSessionView(
                s.getId(), s.getTontineId(), s.getCycleId(),
                cycle != null ? cycle.getNumber() : 0,
                cycle != null ? cycle.getStatus() : null,
                s.getNumber(),
                s.getScheduledAt(), s.getStartedAt(), s.getEndedAt(), s.getLocation(),
                s.getStatus(), s.getBeneficiaryFullName(),
                agenda,
                draft.map(AgendaDraft::getApprovedAt).orElse(null),
                attendance,
                contributions);
    }
}
