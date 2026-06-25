package cm.ftg.tontine.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ResendOtpRequest(@NotBlank String identifier) {
}
