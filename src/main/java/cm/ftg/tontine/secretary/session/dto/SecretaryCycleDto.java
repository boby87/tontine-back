package cm.ftg.tontine.secretary.session.dto;

import cm.ftg.tontine.tontine.entity.Cycle;
import java.time.LocalDate;
import java.util.UUID;

public record SecretaryCycleDto(
        UUID id,
        int number,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        int totalSessions,
        int completedSessions
) {
    public static SecretaryCycleDto from(Cycle c) {
        return new SecretaryCycleDto(
                c.getId(), c.getNumber(), c.getStartDate(), c.getEndDate(),
                c.isActive(), c.getTotalSessions(), c.getCompletedSessions());
    }
}
