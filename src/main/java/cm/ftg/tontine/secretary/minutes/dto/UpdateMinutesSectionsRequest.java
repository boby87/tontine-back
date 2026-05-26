package cm.ftg.tontine.secretary.minutes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateMinutesSectionsRequest(
        @NotNull @Valid List<SectionInput> sections
) {

    public record SectionInput(
            @NotBlank @Size(max = 80) String key,
            @NotBlank @Size(max = 200) String title,
            @NotNull @Size(max = 4000) String content,
            boolean required
    ) {
    }
}
