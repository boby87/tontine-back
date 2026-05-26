package cm.ftg.tontine.auditor.balancereview.entity;

import cm.ftg.tontine.auditor.balancereview.enums.SessionBalanceReviewDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "session_balance_reviews",
        uniqueConstraints = @UniqueConstraint(name = "uk_balance_review_session",
                columnNames = "session_id"),
        indexes = {
                @Index(name = "idx_balance_review_tontine", columnList = "tontine_id"),
                @Index(name = "idx_balance_review_session", columnList = "session_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class SessionBalanceReview {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "session_number", nullable = false)
    private Integer sessionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionBalanceReviewDecision decision;

    @Column(length = 4000)
    private String observations;

    @Column(length = 4000)
    private String reserves;

    @Column(name = "reviewed_by_user_id", nullable = false)
    private UUID reviewedByUserId;

    @Column(name = "reviewed_by_full_name", nullable = false, length = 160)
    private String reviewedByFullName;

    @CreationTimestamp
    @Column(name = "reviewed_at", updatable = false, nullable = false)
    private Instant reviewedAt;
}
