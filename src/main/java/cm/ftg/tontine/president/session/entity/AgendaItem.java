package cm.ftg.tontine.president.session.entity;

import cm.ftg.tontine.president.session.enums.AgendaItemStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "agenda_items", indexes = {
        @Index(name = "idx_agenda_session", columnList = "session_id")
})
@Getter
@Setter
@NoArgsConstructor
public class AgendaItem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "order_idx", nullable = false)
    private int orderIdx;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AgendaItemStatus status = AgendaItemStatus.PENDING;
}
