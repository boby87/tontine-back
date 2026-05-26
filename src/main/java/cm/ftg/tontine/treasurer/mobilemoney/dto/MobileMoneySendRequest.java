package cm.ftg.tontine.treasurer.mobilemoney.dto;

import cm.ftg.tontine.treasurer.mobilemoney.enums.MobileMoneyProvider;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MobileMoneySendRequest(
        @NotNull MobileMoneyProvider provider,
        @NotBlank String toPhone,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank String purpose,
        String pin
) {
}
