package cm.ftg.tontine.president.session.entity;

import cm.ftg.tontine.president.session.enums.AttendanceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "session_attendance", indexes = {
        @Index(name = "idx_attendance_session", columnList = "session_id"),
        @Index(name = "idx_attendance_member", columnList = "member_id")
})
@Getter
@Setter
@NoArgsConstructor
public class SessionAttendanceEntry {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status = AttendanceStatus.ABSENT;

    @Column(name = "check_in_at")
    private Instant checkInAt;
}
