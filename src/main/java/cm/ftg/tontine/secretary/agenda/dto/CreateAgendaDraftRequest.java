package cm.ftg.tontine.secretary.agenda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateAgendaDraftRequest(
        @NotNull UUID sessionId,
        @Positive int sessionNumber,
        @NotNull Instant scheduledAt,
        @Size(max = 200) String location,
        UUID beneficiaryMemberId,
        @NotNull @Size(min = 1) List<@Valid CreateAgendaDraftItemRequest> items
) {
}
