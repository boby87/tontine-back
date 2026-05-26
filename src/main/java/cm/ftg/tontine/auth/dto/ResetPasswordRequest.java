package cm.ftg.tontine.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String identifier,
        @NotBlank @Pattern(regexp = "^\\d{6}$") String code,
        @NotBlank @Size(min = 8, max = 100) String newPassword
) {
}
