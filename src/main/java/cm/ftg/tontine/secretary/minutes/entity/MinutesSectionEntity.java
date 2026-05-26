package cm.ftg.tontine.secretary.minutes.entity;

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
@Table(name = "minutes_sections", indexes = {
        @Index(name = "idx_minutes_section_draft", columnList = "minutes_draft_id"),
        @Index(name = "idx_minutes_section_order", columnList = "order_idx")
})
@Getter
@Setter
@NoArgsConstructor
public class MinutesSectionEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "minutes_draft_id", nullable = false)
    private UUID minutesDraftId;

    @Column(name = "section_key", nullable = false, length = 80)
    private String sectionKey;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(nullable = false)
    private boolean required = false;

    @Column(name = "order_idx", nullable = false)
    private int orderIdx;
}
