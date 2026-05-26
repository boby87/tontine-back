package cm.ftg.tontine.president.delegation.dto;

import cm.ftg.tontine.common.enums.UserRole;
import cm.ftg.tontine.president.delegation.entity.Delegation;
import cm.ftg.tontine.president.delegation.enums.DelegationPower;
import cm.ftg.tontine.president.delegation.enums.DelegationStatus;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public record DelegationDto(
        UUID id,
        UUID tontineId,
        UUID delegateeUserId,
        String delegateeFullName,
        UserRole delegateeRole,
        Set<DelegationPower> powers,
        String reason,
        Instant startsAt,
        Instant endsAt,
        DelegationStatus status,
        Instant createdAt,
        Instant revokedAt,
        String revokedReason
) {

    public static DelegationDto from(Delegation d) {
        Set<DelegationPower> powers = (d.getPowers() == null || d.getPowers().isEmpty())
                ? EnumSet.noneOf(DelegationPower.class)
                : EnumSet.copyOf(d.getPowers());
        return new DelegationDto(
                d.getId(), d.getTontineId(), d.getDelegateeUserId(), d.getDelegateeFullName(),
                d.getDelegateeRole(), powers, d.getReason(), d.getStartsAt(), d.getEndsAt(),
                d.getStatus(), d.getCreatedAt(), d.getRevokedAt(), d.getRevokedReason());
    }
}
