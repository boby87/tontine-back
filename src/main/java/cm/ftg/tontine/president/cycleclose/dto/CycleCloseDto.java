package cm.ftg.tontine.president.cycleclose.dto;

import cm.ftg.tontine.president.cycleclose.entity.CycleClose;
import cm.ftg.tontine.president.cycleclose.enums.CycleCloseStatus;
import cm.ftg.tontine.president.cycleclose.enums.NextCycleDrawMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CycleCloseDto(
        UUID id,
        UUID tontineId,
        UUID cycleId,
        int cycleNumber,
        CycleCloseStatus status,
        List<CycleCloseChecklistItemDto> checklist,
        CycleCloseSummaryDto summary,
        Instant auditorValidatedAt,
        Instant presidentSignedAt,
        Instant closedAt,
        LocalDate nextCycleStartDate,
        NextCycleDrawMode nextCycleDrawMode
) {

    public static CycleCloseDto from(CycleClose c, List<CycleCloseChecklistItemDto> checklist) {
        return new CycleCloseDto(
                c.getId(), c.getTontineId(), c.getCycleId(), c.getCycleNumber(),
                c.getStatus(), checklist, CycleCloseSummaryDto.from(c),
                c.getAuditorValidatedAt(), c.getPresidentSignedAt(), c.getClosedAt(),
                c.getNextCycleStartDate(), c.getNextCycleDrawMode());
    }
}
