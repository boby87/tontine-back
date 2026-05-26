package cm.ftg.tontine.censor.sanction.dto;

import cm.ftg.tontine.president.sanction.enums.SanctionSeverity;
import cm.ftg.tontine.president.sanction.enums.SanctionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

public record ApplySanctionRequest(
        @NotNull UUID memberId,
        @NotNull SanctionType type,
        @PositiveOrZero BigDecimal amount,
        @NotBlank String reason,
        SanctionSeverity severity,
        String customLabel,
        Boolean isFinancial,
        UUID sessionId
) {}
