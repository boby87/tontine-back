package cm.ftg.tontine.secretary.convocation.dto;

import cm.ftg.tontine.secretary.convocation.enums.ConvocationChannel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record CreateConvocationRequest(
        @NotNull UUID sessionId,
        @NotEmpty Set<ConvocationChannel> channels,
        @NotEmpty Set<UUID> audienceMemberIds,
        Boolean includeCandidates,
        @NotBlank @Size(max = 4000) String message,
        List<@Valid CreateReminderRequest> reminders,
        Instant scheduledAt
) {
}
