package cm.ftg.tontine.member.dto;

import cm.ftg.tontine.treasurer.common.enums.PaymentMethod;
import cm.ftg.tontine.treasurer.contribution.entity.Contribution;
import cm.ftg.tontine.treasurer.contribution.enums.ContributionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MemberContributionEntry(
        UUID memberId,
        String memberName,
        BigDecimal expectedAmount,
        BigDecimal paidAmount,
        ContributionStatus status,
        Instant paidAt,
        PaymentMethod paymentMethod
) {
    public static MemberContributionEntry from(Contribution c, String memberName) {
        return new MemberContributionEntry(
                c.getMemberId(),
                memberName,
                c.getExpectedAmount(),
                c.getPaidAmount(),
                c.getStatus(),
                c.getPaidAt(),
                c.getPaymentMethod());
    }
}
