package cm.ftg.tontine.secretary.convocation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateReminderRequest(
        @NotNull @Positive Integer offsetHoursBefore
) {
}
