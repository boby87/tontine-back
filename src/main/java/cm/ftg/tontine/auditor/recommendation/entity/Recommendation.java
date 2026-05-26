package cm.ftg.tontine.auditor.recommendation.entity;

import cm.ftg.tontine.auditor.recommendation.enums.RecommendationOrigin;
import cm.ftg.tontine.auditor.recommendation.enums.RecommendationPriority;
import cm.ftg.tontine.auditor.recommendation.enums.RecommendationRecipient;
import cm.ftg.tontine.auditor.recommendation.enums.RecommendationStatus;
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
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "recommendations", indexes = {
        @Index(name = "idx_recommendation_tontine", columnList = "tontine_id"),
        @Index(name = "idx_recommendation_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class Recommendation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecommendationOrigin origin;

    @Column(name = "origin_id")
    private UUID originId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationRecipient recipient;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationStatus status = RecommendationStatus.PENDING;

    @Column(nullable = false)
    private int progress = 0;

    @Column(name = "close_note", length = 2000)
    private String closeNote;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_by_full_name", nullable = false, length = 160)
    private String createdByFullName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
