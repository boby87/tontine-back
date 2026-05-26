package cm.ftg.tontine.secretary.minutes.dto;

public record AttendanceSummaryDto(
        int present,
        int late,
        int absent,
        int excused,
        int total,
        boolean quorumReached
) {
}
