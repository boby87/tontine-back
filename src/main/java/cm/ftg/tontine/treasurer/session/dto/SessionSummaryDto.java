package cm.ftg.tontine.treasurer.session.dto;

import cm.ftg.tontine.president.session.entity.Session;
import cm.ftg.tontine.president.session.enums.SessionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SessionSummaryDto(
        UUID id,
        UUID tontineId,
        UUID cycleId,
        int number,
        Instant scheduledAt,
        Instant startedAt,
        Instant endedAt,
        SessionStatus status,
        BigDecimal totalCollected,
        BigDecimal totalDistributed,
        UUID beneficiaryMemberId,
        String beneficiaryFullName,
        BigDecimal cagnotteAmount
) {

    public static SessionSummaryDto from(Session s) {
        return new SessionSummaryDto(
                s.getId(), s.getTontineId(), s.getCycleId(), s.getNumber(),
                s.getScheduledAt(), s.getStartedAt(), s.getEndedAt(), s.getStatus(),
                s.getTotalCollected(), s.getTotalDistributed(),
                s.getBeneficiaryMemberId(), s.getBeneficiaryFullName(),
                s.getCagnotteAmount());
    }
}
