package cm.ftg.tontine.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TotpVerifyLoginRequest(
        @NotBlank String totpPendingToken,
        @NotBlank @Pattern(regexp = "\\d{6}") String code
) {}
