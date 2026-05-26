package cm.ftg.tontine.tontine.dto;

import cm.ftg.tontine.tontine.entity.Cycle;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CycleDto(
        UUID id,
        UUID tontineId,
        int number,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        int totalSessions,
        int completedSessions,
        BigDecimal totalCollected
) {

    public static CycleDto from(Cycle c) {
        return new CycleDto(
                c.getId(),
                c.getTontineId(),
                c.getNumber(),
                c.getStartDate(),
                c.getEndDate(),
                c.isActive(),
                c.getTotalSessions(),
                c.getCompletedSessions(),
                c.getTotalCollected());
    }
}
