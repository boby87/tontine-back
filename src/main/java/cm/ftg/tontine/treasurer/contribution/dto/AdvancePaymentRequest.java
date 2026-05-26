package cm.ftg.tontine.treasurer.contribution.dto;

import cm.ftg.tontine.treasurer.common.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AdvancePaymentRequest(
        @NotNull UUID memberId,
        @NotEmpty List<UUID> sessionIds,
        @NotNull @DecimalMin(value = "0.01", message = "Le montant doit etre superieur a zero")
        BigDecimal amount,
        @NotNull PaymentMethod paymentMethod
) {
}
