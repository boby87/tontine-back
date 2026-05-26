package cm.ftg.tontine.secretary.agenda.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "agenda_draft_items", indexes = {
        @Index(name = "idx_agenda_draft_items_draft", columnList = "agenda_draft_id")
})
@Getter
@Setter
@NoArgsConstructor
public class AgendaDraftItem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "agenda_draft_id", nullable = false)
    private UUID agendaDraftId;

    @Column(name = "order_idx", nullable = false)
    private int orderIdx;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "is_standard", nullable = false)
    private boolean isStandard;

    @Column(name = "estimated_duration_min")
    private Integer estimatedDurationMin;

    @Column(name = "proposed_by", length = 160)
    private String proposedBy;
}
