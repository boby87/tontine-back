package cm.ftg.tontine.president.cycleclose.entity;

import cm.ftg.tontine.president.cycleclose.enums.ChecklistItemStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cycle_close_checklist",
        uniqueConstraints = @UniqueConstraint(name = "uk_cycle_close_item",
                columnNames = {"cycle_close_id", "item_key"}),
        indexes = {
                @Index(name = "idx_cycle_close_item_close", columnList = "cycle_close_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class CycleCloseChecklistItem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "cycle_close_id", nullable = false)
    private UUID cycleCloseId;

    @Column(name = "item_key", nullable = false, length = 60)
    private String itemKey;

    @Column(nullable = false, length = 200)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChecklistItemStatus status = ChecklistItemStatus.PENDING;

    @Column(name = "blocking_reason", length = 500)
    private String blockingReason;
}
