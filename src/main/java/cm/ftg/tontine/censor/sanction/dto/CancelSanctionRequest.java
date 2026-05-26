package cm.ftg.tontine.censor.sanction.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelSanctionRequest(@NotBlank String reason) {}
