package cm.ftg.tontine.secretary.convocation.dto;

import cm.ftg.tontine.secretary.convocation.entity.ReminderEmbeddable;

public record ReminderDto(int offsetHoursBefore) {

    public static ReminderDto from(ReminderEmbeddable r) {
        return new ReminderDto(r.getOffsetHoursBefore());
    }
}
