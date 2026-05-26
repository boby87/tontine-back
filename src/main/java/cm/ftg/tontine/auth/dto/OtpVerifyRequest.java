package cm.ftg.tontine.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpVerifyRequest(
        @NotBlank String identifier,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "Le code OTP doit comporter 6 chiffres") String code
) {
}
