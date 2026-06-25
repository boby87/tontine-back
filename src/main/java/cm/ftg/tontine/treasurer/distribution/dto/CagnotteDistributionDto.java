package cm.ftg.tontine.treasurer.distribution.dto;

import cm.ftg.tontine.common.enums.DistributionMode;
import cm.ftg.tontine.treasurer.common.enums.PaymentMethod;
import cm.ftg.tontine.treasurer.distribution.entity.CagnotteDistribution;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CagnotteDistributionDto(
        UUID id,
        UUID sessionId,
        int sessionNumber,
        UUID cycleId,
        UUID tontineId,
        UUID beneficiaryMemberId,
        String beneficiaryFullName,
        String beneficiaryPhone,
        BigDecimal grossAmount,
        BigDecimal deductionEmergency,
        BigDecimal deductionOperations,
        BigDecimal netAmount,
        DistributionMode distributionMode,
        BigDecimal auctionSacrificeAmount,
        PaymentMethod paymentMethod,
        boolean beneficiaryConfirmed,
        Instant beneficiaryConfirmedAt,
        Instant treasurerPaidAt
) {

    public static CagnotteDistributionDto from(CagnotteDistribution d) {
        return new CagnotteDistributionDto(d.getId(), d.getSessionId(), d.getSessionNumber(), d.getCycleId(),
                d.getTontineId(), d.getBeneficiaryMemberId(), d.getBeneficiaryFullName(), d.getBeneficiaryPhone(),
                d.getGrossAmount(), d.getDeductionEmergency(), d.getDeductionOperations(), d.getNetAmount(),
                d.getDistributionMode(), d.getAuctionSacrificeAmount(),
                d.getPaymentMethod(), d.isBeneficiaryConfirmed(), d.getBeneficiaryConfirmedAt(),
                d.getTreasurerPaidAt());
    }
}
