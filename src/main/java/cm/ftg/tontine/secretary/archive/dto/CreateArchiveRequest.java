package cm.ftg.tontine.secretary.archive.dto;

import cm.ftg.tontine.secretary.archive.enums.ArchiveDocumentType;
import cm.ftg.tontine.secretary.archive.enums.ArchiveVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateArchiveRequest(
        @NotNull ArchiveDocumentType type,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 255) String fileName,
        @NotNull @PositiveOrZero Long fileSize,
        @NotNull ArchiveVisibility visibility,
        Integer cycleNumber,
        Integer sessionNumber,
        List<@Size(max = 80) String> tags
) {
}
