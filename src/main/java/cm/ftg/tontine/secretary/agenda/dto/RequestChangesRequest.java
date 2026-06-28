package cm.ftg.tontine.secretary.agenda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestChangesRequest(
        @NotBlank @Size(max = 2000) String comment
) {}
