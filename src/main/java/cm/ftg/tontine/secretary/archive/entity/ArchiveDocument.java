package cm.ftg.tontine.secretary.archive.entity;

import cm.ftg.tontine.secretary.archive.enums.ArchiveDocumentType;
import cm.ftg.tontine.secretary.archive.enums.ArchiveVisibility;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "archive_documents", indexes = {
        @Index(name = "idx_archive_tontine", columnList = "tontine_id"),
        @Index(name = "idx_archive_type", columnList = "type"),
        @Index(name = "idx_archive_cycle", columnList = "cycle_number")
})
@Getter
@Setter
@NoArgsConstructor
public class ArchiveDocument {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ArchiveDocumentType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ArchiveVisibility visibility = ArchiveVisibility.ALL_MEMBERS;

    @Column(name = "cycle_number")
    private Integer cycleNumber;

    @Column(name = "session_number")
    private Integer sessionNumber;

    @Column(name = "uploaded_by_user_id", nullable = false)
    private UUID uploadedByUserId;

    @Column(name = "uploaded_by_full_name", nullable = false, length = 160)
    private String uploadedByFullName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "archive_document_tags",
            joinColumns = @JoinColumn(name = "archive_document_id"),
            indexes = @Index(name = "idx_archive_tags_doc", columnList = "archive_document_id"))
    @Column(name = "tag", length = 80, nullable = false)
    private List<String> tags = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false, nullable = false)
    private Instant uploadedAt;
}
