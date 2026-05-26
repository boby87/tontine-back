package cm.ftg.tontine.president.extracontrib.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "extra_contribution_members", indexes = {
        @Index(name = "idx_extra_member_extra", columnList = "extra_contrib_id"),
        @Index(name = "idx_extra_member_member", columnList = "member_id")
})
@Getter
@Setter
@NoArgsConstructor
public class ExtraContributionMember {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "extra_contrib_id", nullable = false)
    private UUID extraContribId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal expected = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal paid = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean exempted = false;

    @Column(name = "paid_at")
    private Instant paidAt;
}
