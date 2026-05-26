package cm.ftg.tontine.treasurer.cashbox.entity;

import cm.ftg.tontine.treasurer.cashbox.enums.CashMovementKind;
import cm.ftg.tontine.treasurer.cashbox.enums.MovementDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "cash_movements", indexes = {
        @Index(name = "idx_cashmove_tontine", columnList = "tontine_id"),
        @Index(name = "idx_cashmove_cashbox", columnList = "cash_box_id"),
        @Index(name = "idx_cashmove_kind", columnList = "kind"),
        @Index(name = "idx_cashmove_recorded_at", columnList = "recorded_at")
})
@Getter
@Setter
@NoArgsConstructor
public class CashMovement {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(name = "cash_box_id", nullable = false)
    private UUID cashBoxId;

    @Column(name = "cash_box_name", nullable = false, length = 120)
    private String cashBoxName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CashMovementKind kind;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private MovementDirection direction;

    @Column(length = 160)
    private String reference;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "recorded_by_full_name", nullable = false, length = 160)
    private String recordedByFullName;

    @CreationTimestamp
    @Column(name = "recorded_at", updatable = false, nullable = false)
    private Instant recordedAt;
}
