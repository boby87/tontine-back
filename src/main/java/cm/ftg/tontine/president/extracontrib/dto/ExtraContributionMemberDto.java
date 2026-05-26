package cm.ftg.tontine.president.extracontrib.dto;

import cm.ftg.tontine.president.extracontrib.entity.ExtraContributionMember;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExtraContributionMemberDto(
        UUID memberId,
        String fullName,
        BigDecimal expected,
        BigDecimal paid,
        boolean exempted,
        Instant paidAt
) {

    public static ExtraContributionMemberDto from(ExtraContributionMember m) {
        return new ExtraContributionMemberDto(
                m.getMemberId(), m.getFullName(), m.getExpected(), m.getPaid(),
                m.isExempted(), m.getPaidAt());
    }
}
