package cm.ftg.tontine.secretary.convocation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReminderEmbeddable {

    @Column(name = "offset_hours_before", nullable = false)
    private Integer offsetHoursBefore;
}
