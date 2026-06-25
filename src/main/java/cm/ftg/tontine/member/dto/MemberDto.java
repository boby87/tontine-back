package cm.ftg.tontine.member.dto;

import cm.ftg.tontine.common.enums.MemberStatus;
import cm.ftg.tontine.common.enums.UserRole;
import cm.ftg.tontine.member.entity.Member;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record MemberDto(
        UUID id,
        UUID userId,
        UUID tontineId,
        String matricule,
        String firstName,
        String lastName,
        String phone,
        String email,
        String avatarUrl,
        MemberStatus status,
        Set<UserRole> roles,
        Instant joinedAt,
        Integer rotationOrder,
        boolean hasReceivedTour,
        BigDecimal totalContributed,
        BigDecimal totalArrears
) {

    public static MemberDto from(Member m) {
        return new MemberDto(
                m.getId(),
                m.getUserId(),
                m.getTontineId(),
                m.getMatricule(),
                m.getFirstName(),
                m.getLastName(),
                m.getPhone(),
                m.getEmail(),
                m.getAvatarUrl(),
                m.getStatus(),
                m.getRoles(),
                m.getJoinedAt(),
                m.getRotationOrder(),
                m.isHasReceivedTour(),
                m.getTotalContributed(),
                m.getTotalArrears());
    }
}
