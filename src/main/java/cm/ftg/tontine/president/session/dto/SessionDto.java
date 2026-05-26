package cm.ftg.tontine.president.session.dto;

import cm.ftg.tontine.president.session.entity.Session;
import cm.ftg.tontine.president.session.enums.SessionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionDto(
        UUID id,
        UUID tontineId,
        UUID cycleId,
        int number,
        Instant scheduledAt,
        Instant startedAt,
        Instant endedAt,
        String location,
        SessionStatus status,
        List<AgendaItemDto> agenda,
        List<SessionAttendanceEntryDto> attendance,
        BigDecimal totalCollected,
        BigDecimal totalDistributed,
        BigDecimal quorumThreshold,
        UUID beneficiaryMemberId,
        String beneficiaryFullName,
        BigDecimal cagnotteAmount,
        boolean cagnotteSignedByPresident,
        Instant nextSessionDate
) {

    public static SessionDto from(Session s,
                                  List<AgendaItemDto> agenda,
                                  List<SessionAttendanceEntryDto> attendance) {
        return new SessionDto(
                s.getId(), s.getTontineId(), s.getCycleId(), s.getNumber(),
                s.getScheduledAt(), s.getStartedAt(), s.getEndedAt(), s.getLocation(), s.getStatus(),
                agenda, attendance,
                s.getTotalCollected(), s.getTotalDistributed(), s.getQuorumThreshold(),
                s.getBeneficiaryMemberId(), s.getBeneficiaryFullName(),
                s.getCagnotteAmount(), s.isCagnotteSignedByPresident(), s.getNextSessionDate());
    }

    public static SessionDto fromSummary(Session s) {
        return from(s, List.of(), List.of());
    }
}
