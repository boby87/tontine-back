package cm.ftg.tontine.treasurer.distribution.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RunLotteryRequest(@NotNull UUID sessionId) {
}
