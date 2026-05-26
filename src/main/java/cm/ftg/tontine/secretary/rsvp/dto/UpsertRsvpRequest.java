package cm.ftg.tontine.secretary.rsvp.dto;

import cm.ftg.tontine.secretary.rsvp.enums.RsvpStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertRsvpRequest(
        @NotNull RsvpStatus status,
        @Size(max = 2000) String reason
) {
}
