package cm.ftg.tontine.censor.report.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateObservationsRequest(@NotBlank String observations) {}
