package cm.ftg.tontine.secretary.session.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateCycleRequest(
        @NotNull LocalDate startDate
) {
}
