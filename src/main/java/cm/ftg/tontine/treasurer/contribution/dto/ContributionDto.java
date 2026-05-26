package cm.ftg.tontine.treasurer.contribution.dto;

import cm.ftg.tontine.treasurer.common.enums.PaymentMethod;
import cm.ftg.tontine.treasurer.contribution.entity.Contribution;
import cm.ftg.tontine.treasurer.contribution.enums.ContributionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ContributionDto(
        UUID id,
        UUID tontineId,
        UUID sessionId,
        UUID memberId,
        BigDecimal expectedAmount,
        BigDecimal paidAmount,
        ContributionStatus status,
        Instant paidAt,
        PaymentMethod paymentMethod,
        String reference,
        UUID collectedByUserId,
        String note
) {

    public static ContributionDto from(Contribution c) {
        return new ContributionDto(
                c.getId(), c.getTontineId(), c.getSessionId(), c.getMemberId(),
                c.getExpectedAmount(), c.getPaidAmount(), c.getStatus(), c.getPaidAt(),
                c.getPaymentMethod(), c.getReference(), c.getCollectedByUserId(), c.getNote());
    }
}
