package cm.ftg.tontine.secretary.attendance.dto;

import cm.ftg.tontine.president.session.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

public record UpsertAttendanceRequest(
        @NotNull AttendanceStatus status
) {
}
