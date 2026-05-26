package cm.ftg.tontine.treasurer.mobilemoney.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectMobileMoneyRequest(@NotBlank String reason) {
}
