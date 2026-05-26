package cm.ftg.tontine.treasurer.extracontrib.dto;

import cm.ftg.tontine.treasurer.common.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CollectExtraContributionRequest(
        @NotNull UUID memberId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull PaymentMethod paymentMethod
) {
}
