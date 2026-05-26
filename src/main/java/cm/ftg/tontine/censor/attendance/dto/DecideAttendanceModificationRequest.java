package cm.ftg.tontine.censor.attendance.dto;

import cm.ftg.tontine.censor.attendance.enums.AttendanceModificationDecision;
import jakarta.validation.constraints.NotNull;

public record DecideAttendanceModificationRequest(
        @NotNull AttendanceModificationDecision decision,
        String comment,
        String infoRequest
) {
}
