package cm.ftg.tontine.treasurer.distribution.dto;

import cm.ftg.tontine.common.enums.DistributionMode;
import java.math.BigDecimal;
import java.util.UUID;

public record BeneficiarySelectionDto(
        UUID sessionId,
        UUID beneficiaryMemberId,
        String beneficiaryFullName,
        String beneficiaryPhone,
        DistributionMode mode,
        BigDecimal sacrificeAmount
) {
}
