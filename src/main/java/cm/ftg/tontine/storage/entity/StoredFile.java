package cm.ftg.tontine.storage.entity;

import cm.ftg.tontine.storage.enums.StorageCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "stored_files", indexes = {
        @Index(name = "idx_stored_file_tontine", columnList = "tontine_id"),
        @Index(name = "idx_stored_file_owner", columnList = "owner_user_id"),
        @Index(name = "idx_stored_file_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
public class StoredFile {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id")
    private UUID tontineId;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StorageCategory category;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;
}
