package cm.ftg.tontine.member.dto;

import cm.ftg.tontine.president.session.entity.SessionAttendanceEntry;
import cm.ftg.tontine.president.session.enums.AttendanceStatus;
import java.time.Instant;
import java.util.UUID;

public record MemberAttendanceEntry(
        UUID memberId,
        String fullName,
        AttendanceStatus status,
        Instant checkInAt
) {
    public static MemberAttendanceEntry from(SessionAttendanceEntry e) {
        return new MemberAttendanceEntry(e.getMemberId(), e.getFullName(), e.getStatus(), e.getCheckInAt());
    }
}
