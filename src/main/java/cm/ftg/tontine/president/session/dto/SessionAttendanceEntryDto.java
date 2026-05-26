package cm.ftg.tontine.president.session.dto;

import cm.ftg.tontine.president.session.entity.SessionAttendanceEntry;
import cm.ftg.tontine.president.session.enums.AttendanceStatus;
import java.time.Instant;
import java.util.UUID;

public record SessionAttendanceEntryDto(
        UUID memberId,
        String fullName,
        AttendanceStatus status,
        Instant checkInAt
) {

    public static SessionAttendanceEntryDto from(SessionAttendanceEntry e) {
        return new SessionAttendanceEntryDto(e.getMemberId(), e.getFullName(), e.getStatus(), e.getCheckInAt());
    }
}
