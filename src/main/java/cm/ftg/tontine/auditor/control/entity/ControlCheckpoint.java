package cm.ftg.tontine.auditor.control.entity;

import cm.ftg.tontine.auditor.control.enums.CheckpointCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "control_checkpoints", indexes = {
        @Index(name = "idx_checkpoint_control", columnList = "control_id")
})
@Getter
@Setter
@NoArgsConstructor
public class ControlCheckpoint {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "control_id", nullable = false)
    private UUID controlId;

    @Column(nullable = false, length = 200)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CheckpointCategory category;

    @Column(name = "expected_value", precision = 19, scale = 2)
    private BigDecimal expectedValue;

    @Column(name = "observed_value", precision = 19, scale = 2)
    private BigDecimal observedValue;

    @Column(precision = 19, scale = 2)
    private BigDecimal variance;

    @Column
    private Boolean conform;

    @Column(length = 2000)
    private String note;

    @Column(name = "order_idx", nullable = false)
    private int orderIdx;
}
