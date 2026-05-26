package cm.ftg.tontine.treasurer.cashbox.entity;

import cm.ftg.tontine.treasurer.cashbox.enums.CashBoxType;
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
@Table(name = "cash_boxes", indexes = {
        @Index(name = "idx_cashbox_tontine", columnList = "tontine_id"),
        @Index(name = "idx_cashbox_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
public class CashBox {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CashBoxType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "is_locked", nullable = false)
    private boolean isLocked = false;

    @Column(name = "threshold_min", precision = 19, scale = 2)
    private BigDecimal thresholdMin;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Version
    private Long version;
}
