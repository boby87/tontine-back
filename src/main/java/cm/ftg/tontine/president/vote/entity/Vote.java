package cm.ftg.tontine.president.vote.entity;

import cm.ftg.tontine.president.vote.enums.VoteAudience;
import cm.ftg.tontine.president.vote.enums.VoteScope;
import cm.ftg.tontine.president.vote.enums.VoteStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "votes", indexes = {
        @Index(name = "idx_vote_tontine", columnList = "tontine_id"),
        @Index(name = "idx_vote_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class Vote {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(nullable = false, length = 500)
    private String question;

    @Column(length = 2000)
    private String description;

    @Column(name = "is_anonymous", nullable = false)
    private boolean anonymous = false;

    @Column(name = "hide_results_until_close", nullable = false)
    private boolean hideResultsUntilClose = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoteScope scope = VoteScope.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoteAudience audience = VoteAudience.ALL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoteStatus status = VoteStatus.DRAFT;

    @Column(name = "opens_at", nullable = false)
    private Instant opensAt;

    @Column(name = "closes_at", nullable = false)
    private Instant closesAt;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_by_full_name", nullable = false, length = 160)
    private String createdByFullName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "total_voters", nullable = false)
    private int totalVoters = 0;

    @Column(name = "total_voted", nullable = false)
    private int totalVoted = 0;

    @Column(name = "quorum_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal quorumPercent = BigDecimal.ZERO;

    @Column(name = "passed")
    private Boolean passed;

    @Version
    private Long version;
}
