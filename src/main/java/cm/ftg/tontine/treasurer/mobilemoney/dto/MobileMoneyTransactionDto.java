package cm.ftg.tontine.treasurer.mobilemoney.dto;

import cm.ftg.tontine.treasurer.cashbox.enums.MovementDirection;
import cm.ftg.tontine.treasurer.mobilemoney.entity.MobileMoneyTransaction;
import cm.ftg.tontine.treasurer.mobilemoney.enums.MobileMoneyProvider;
import cm.ftg.tontine.treasurer.mobilemoney.enums.MobileMoneyStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MobileMoneyTransactionDto(
        UUID id,
        UUID tontineId,
        MobileMoneyProvider provider,
        MovementDirection direction,
        BigDecimal amount,
        String fromPhone,
        String toPhone,
        String externalReference,
        UUID matchedMemberId,
        String matchedMemberFullName,
        UUID contributionId,
        MobileMoneyStatus status,
        Instant receivedAt,
        Instant reviewedAt,
        String reviewedByFullName,
        String rejectionReason
) {

    public static MobileMoneyTransactionDto from(MobileMoneyTransaction t) {
        return new MobileMoneyTransactionDto(
                t.getId(), t.getTontineId(), t.getProvider(), t.getDirection(),
                t.getAmount(), t.getFromPhone(), t.getToPhone(), t.getExternalReference(),
                t.getMatchedMemberId(), t.getMatchedMemberFullName(), t.getContributionId(),
                t.getStatus(), t.getReceivedAt(), t.getReviewedAt(),
                t.getReviewedByFullName(), t.getRejectionReason());
    }
}
