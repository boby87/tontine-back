package cm.ftg.tontine.president.extracontrib.dto;

import cm.ftg.tontine.president.extracontrib.entity.ExtraordinaryContribution;
import cm.ftg.tontine.president.extracontrib.enums.ExtraContributionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExtraordinaryContributionDto(
        UUID id,
        UUID tontineId,
        String motive,
        UUID beneficiaryMemberId,
        String beneficiaryFullName,
        BigDecimal amountPerMember,
        LocalDate dueDate,
        ExtraContributionStatus status,
        boolean exemptBeneficiary,
        BigDecimal totalExpected,
        BigDecimal totalCollected,
        List<ExtraContributionMemberDto> members,
        Instant votedByAssemblyAt,
        Instant createdAt,
        Instant closedAt,
        Instant distributedAt
) {

    public static ExtraordinaryContributionDto from(ExtraordinaryContribution e, List<ExtraContributionMemberDto> members) {
        return new ExtraordinaryContributionDto(
                e.getId(), e.getTontineId(), e.getMotive(),
                e.getBeneficiaryMemberId(), e.getBeneficiaryFullName(),
                e.getAmountPerMember(), e.getDueDate(), e.getStatus(),
                e.isExemptBeneficiary(), e.getTotalExpected(), e.getTotalCollected(),
                members, e.getVotedByAssemblyAt(), e.getCreatedAt(),
                e.getClosedAt(), e.getDistributedAt());
    }
}
