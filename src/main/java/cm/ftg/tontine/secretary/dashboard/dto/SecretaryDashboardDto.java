package cm.ftg.tontine.secretary.dashboard.dto;

import cm.ftg.tontine.secretary.rsvp.dto.SessionRsvpSummaryDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SecretaryDashboardDto(
        NextSessionDto nextSession,
        SessionRsvpSummaryDto rsvpSummary,
        SecretaryKpiDto kpi,
        List<ActivityEntryDto> recentActivity
) {

    public record NextSessionDto(
            UUID id,
            int number,
            Instant scheduledAt,
            String location,
            long daysUntil
    ) {
    }

    public record SecretaryKpiDto(
            long minutesPending,
            long agendaPending,
            long pendingAdhesions,
            long pendingResignations,
            long totalMembers,
            long archivesCount
    ) {
    }

    public record ActivityEntryDto(
            UUID id,
            String label,
            String status,
            Instant updatedAt
    ) {
    }
}
