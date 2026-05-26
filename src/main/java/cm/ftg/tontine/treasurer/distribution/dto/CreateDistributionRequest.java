package cm.ftg.tontine.treasurer.distribution.dto;

import cm.ftg.tontine.treasurer.common.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record CreateDistributionRequest(
        @NotNull UUID sessionId,
        @NotNull PaymentMethod paymentMethod,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "OTP doit etre un code numerique de 6 chiffres") String otp
) {
}
