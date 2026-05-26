package cm.ftg.tontine.censor.dashboard.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CensorDashboardDto(
        AlertsDto alerts,
        SanctionsPeriodDto sanctionsThisPeriod,
        List<TopSanctionedDto> topSanctioned,
        NextSessionDto session
) {

    public record AlertsDto(
            long pendingAttendance,
            long pendingJustifications,
            long pendingContestations,
            long unpaidCount,
            BigDecimal unpaidAmount
    ) {
    }

    public record SanctionsPeriodDto(
            long count,
            BigDecimal amount,
            BigDecimal collected,
            List<SanctionBreakdownDto> breakdown
    ) {
    }

    public record SanctionBreakdownDto(
            String type,
            long count,
            BigDecimal amount
    ) {
    }

    public record TopSanctionedDto(
            UUID memberId,
            String name,
            long count,
            BigDecimal amount
    ) {
    }

    public record NextSessionDto(
            UUID id,
            int number,
            String status,
            Instant scheduledAt
    ) {
    }
}
