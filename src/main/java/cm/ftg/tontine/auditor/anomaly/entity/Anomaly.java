package cm.ftg.tontine.auditor.anomaly.entity;

import cm.ftg.tontine.auditor.anomaly.enums.AnomalyAudience;
import cm.ftg.tontine.auditor.anomaly.enums.AnomalyCategory;
import cm.ftg.tontine.auditor.anomaly.enums.AnomalySeverity;
import cm.ftg.tontine.auditor.anomaly.enums.AnomalyStatus;
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
@Table(name = "anomalies", indexes = {
        @Index(name = "idx_anomaly_tontine", columnList = "tontine_id"),
        @Index(name = "idx_anomaly_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class Anomaly {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AnomalyCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnomalySeverity severity;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnomalyAudience audience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnomalyStatus status = AnomalyStatus.OPEN;

    @Column(name = "requests_response", nullable = false)
    private boolean requestsResponse = false;

    @Column(name = "copy_to_treasurer", nullable = false)
    private boolean copyToTreasurer = false;

    @Column(name = "reported_by_user_id", nullable = false)
    private UUID reportedByUserId;

    @Column(name = "reported_by_full_name", length = 160)
    private String reportedByFullName;

    @CreationTimestamp
    @Column(name = "reported_at", updatable = false, nullable = false)
    private Instant reportedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "resolution_comment", length = 2000)
    private String resolutionComment;

    @Version
    private Long version;
}
