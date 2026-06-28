package cm.ftg.tontine.secretary.session.dto;

import cm.ftg.tontine.tontine.entity.Cycle;
import cm.ftg.tontine.tontine.enums.CycleStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SecretaryCycleDto(
        UUID id,
        UUID tontineId,
        int number,
        LocalDate startDate,
        LocalDate endDate,
        CycleStatus status,
        boolean isActive,
        int totalSessions,
        int completedSessions,
        BigDecimal totalCollected
) {
    public static SecretaryCycleDto from(Cycle c) {
        return new SecretaryCycleDto(
                c.getId(), c.getTontineId(), c.getNumber(), c.getStartDate(), c.getEndDate(),
                c.getStatus(), c.getStatus() == CycleStatus.ACTIVE,
                c.getTotalSessions(), c.getCompletedSessions(), c.getTotalCollected());
    }
}
