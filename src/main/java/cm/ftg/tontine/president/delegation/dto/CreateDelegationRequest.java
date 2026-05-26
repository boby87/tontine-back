package cm.ftg.tontine.president.delegation.dto;

import cm.ftg.tontine.president.delegation.enums.DelegationPower;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;

public record CreateDelegationRequest(
        @NotNull java.util.UUID delegateeUserId,
        @NotEmpty Set<DelegationPower> powers,
        @NotBlank @Size(max = 1000) String reason,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt
) {
}
