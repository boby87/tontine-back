package cm.ftg.tontine.secretary.agenda.dto;

import cm.ftg.tontine.secretary.agenda.entity.AgendaDraft;
import cm.ftg.tontine.secretary.agenda.enums.AgendaDraftStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgendaDraftDto(
        UUID id,
        UUID tontineId,
        UUID sessionId,
        int sessionNumber,
        Instant scheduledAt,
        String location,
        UUID beneficiaryMemberId,
        String beneficiaryFullName,
        List<AgendaDraftItemDto> items,
        AgendaDraftStatus status,
        Instant createdAt,
        Instant submittedAt,
        Instant approvedAt,
        Instant publishedAt,
        String presidentComment
) {

    public static AgendaDraftDto from(AgendaDraft d, List<AgendaDraftItemDto> items) {
        return new AgendaDraftDto(
                d.getId(),
                d.getTontineId(),
                d.getSessionId(),
                d.getSessionNumber(),
                d.getScheduledAt(),
                d.getLocation(),
                d.getBeneficiaryMemberId(),
                d.getBeneficiaryFullName(),
                items,
                d.getStatus(),
                d.getCreatedAt(),
                d.getSubmittedAt(),
                d.getApprovedAt(),
                d.getPublishedAt(),
                d.getPresidentComment());
    }

    public static AgendaDraftDto fromSummary(AgendaDraft d) {
        return from(d, List.of());
    }
}
