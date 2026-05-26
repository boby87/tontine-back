package cm.ftg.tontine.treasurer.loan.dto;

import cm.ftg.tontine.treasurer.common.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RepayLoanRequest(
        @NotNull @DecimalMin(value = "0.01", message = "Le montant doit etre superieur a zero")
        BigDecimal amount,
        @NotNull PaymentMethod paymentMethod
) {
}
