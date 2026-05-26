package cm.ftg.tontine.tontine.entity;

import cm.ftg.tontine.common.enums.ContributionFrequency;
import cm.ftg.tontine.common.enums.TontineStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "tontines")
@Getter
@Setter
@NoArgsConstructor
public class Tontine {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TontineStatus status = TontineStatus.DRAFT;

    @Column(name = "contribution_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal contributionAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContributionFrequency frequency;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false)
    private int memberCount = 0;

    @Column(nullable = false)
    private int maxMembers;

    @Column(name = "current_cycle_id")
    private UUID currentCycleId;

    @Column(name = "total_saved", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalSaved = BigDecimal.ZERO;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Embedded
    private TontineRulesEmbeddable rules;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "tontine_founders", joinColumns = @JoinColumn(name = "tontine_id"))
    private List<FounderInviteEmbeddable> founders = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
