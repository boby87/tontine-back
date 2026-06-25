package cm.ftg.tontine.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TotpConfirmRequest(
        @NotBlank @Pattern(regexp = "\\d{6}") String code
) {}
