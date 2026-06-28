package cm.ftg.tontine.member.dto;

import cm.ftg.tontine.president.session.enums.SessionStatus;
import cm.ftg.tontine.secretary.agenda.dto.AgendaDraftItemDto;
import cm.ftg.tontine.tontine.enums.CycleStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MemberSessionView(
        UUID id,
        UUID tontineId,
        UUID cycleId,
        int cycleNumber,
        CycleStatus cycleStatus,
        int number,
        Instant scheduledAt,
        Instant startedAt,
        Instant endedAt,
        String location,
        SessionStatus status,
        String beneficiaryFullName,
        List<AgendaDraftItemDto> agenda,
        Instant agendaApprovedAt,
        List<MemberAttendanceEntry> attendance,
        List<MemberContributionEntry> contributions
) {}
