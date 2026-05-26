package cm.ftg.tontine.treasurer.sanction.dto;

import cm.ftg.tontine.treasurer.common.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record CollectSanctionRequest(
        @NotNull PaymentMethod paymentMethod
) {
}
