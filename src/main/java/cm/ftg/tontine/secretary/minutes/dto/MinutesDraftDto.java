package cm.ftg.tontine.secretary.minutes.dto;

import cm.ftg.tontine.secretary.minutes.entity.MinutesDraft;
import cm.ftg.tontine.secretary.minutes.enums.MinutesStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MinutesDraftDto(
        UUID id,
        UUID tontineId,
        UUID sessionId,
        int sessionNumber,
        LocalDate sessionDate,
        AttendanceSummaryDto attendanceSummary,
        FinancialSummaryDto financialSummary,
        List<MinutesSectionDto> sections,
        MinutesStatus status,
        Instant secretarySignedAt,
        Instant presidentSignedAt,
        Instant publishedAt,
        String presidentComment,
        Instant createdAt,
        Instant updatedAt
) {

    public static MinutesDraftDto from(MinutesDraft d, List<MinutesSectionDto> sections) {
        return new MinutesDraftDto(
                d.getId(), d.getTontineId(), d.getSessionId(), d.getSessionNumber(), d.getSessionDate(),
                new AttendanceSummaryDto(d.getAttPresent(), d.getAttLate(), d.getAttAbsent(),
                        d.getAttExcused(), d.getAttTotal(), d.isAttQuorumReached()),
                new FinancialSummaryDto(d.getFinTotalCollected(), d.getFinTotalDistributed(),
                        d.getFinBeneficiaryFullName()),
                sections,
                d.getStatus(),
                d.getSecretarySignedAt(), d.getPresidentSignedAt(), d.getPublishedAt(),
                d.getPresidentComment(),
                d.getCreatedAt(), d.getUpdatedAt());
    }
}
