package cm.ftg.tontine.tontine.entity;

import cm.ftg.tontine.tontine.enums.CycleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cycles", indexes = {
        @Index(name = "idx_cycles_tontine", columnList = "tontine_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Cycle {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(nullable = false)
    private int number;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CycleStatus status = CycleStatus.ACTIVE;

    @Column(nullable = false)
    private int totalSessions = 0;

    @Column(nullable = false)
    private int completedSessions = 0;

    @Column(name = "total_collected", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalCollected = BigDecimal.ZERO;

}
