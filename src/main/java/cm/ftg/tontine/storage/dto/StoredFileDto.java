package cm.ftg.tontine.storage.dto;

import cm.ftg.tontine.storage.entity.StoredFile;
import cm.ftg.tontine.storage.enums.StorageCategory;
import java.time.Instant;
import java.util.UUID;

public record StoredFileDto(
        UUID id,
        UUID tontineId,
        UUID ownerUserId,
        StorageCategory category,
        String originalName,
        String contentType,
        long sizeBytes,
        String sha256,
        Instant createdAt) {

    public static StoredFileDto from(StoredFile f) {
        return new StoredFileDto(
                f.getId(),
                f.getTontineId(),
                f.getOwnerUserId(),
                f.getCategory(),
                f.getOriginalName(),
                f.getContentType(),
                f.getSizeBytes(),
                f.getSha256(),
                f.getCreatedAt());
    }
}
