package cm.ftg.tontine.treasurer.contribution.dto;

import cm.ftg.tontine.treasurer.common.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PayContributionRequest(
        @NotNull @DecimalMin(value = "0.01", message = "Le montant doit etre superieur a zero")
        BigDecimal amount,
        @NotNull PaymentMethod paymentMethod,
        @Size(max = 120) String reference,
        @Size(max = 1000) String note
) {
}
