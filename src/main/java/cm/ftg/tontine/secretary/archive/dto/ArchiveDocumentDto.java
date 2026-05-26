package cm.ftg.tontine.secretary.archive.dto;

import cm.ftg.tontine.secretary.archive.entity.ArchiveDocument;
import cm.ftg.tontine.secretary.archive.enums.ArchiveDocumentType;
import cm.ftg.tontine.secretary.archive.enums.ArchiveVisibility;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArchiveDocumentDto(
        UUID id,
        UUID tontineId,
        ArchiveDocumentType type,
        String title,
        String description,
        String fileName,
        long fileSize,
        ArchiveVisibility visibility,
        Integer cycleNumber,
        Integer sessionNumber,
        UUID uploadedByUserId,
        String uploadedByFullName,
        List<String> tags,
        Instant uploadedAt
) {

    public static ArchiveDocumentDto from(ArchiveDocument d) {
        return new ArchiveDocumentDto(
                d.getId(), d.getTontineId(), d.getType(), d.getTitle(), d.getDescription(),
                d.getFileName(), d.getFileSize(), d.getVisibility(),
                d.getCycleNumber(), d.getSessionNumber(),
                d.getUploadedByUserId(), d.getUploadedByFullName(),
                List.copyOf(d.getTags()), d.getUploadedAt());
    }
}
