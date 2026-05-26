package cm.ftg.tontine.secretary.registry.dto;

import cm.ftg.tontine.common.enums.MemberStatus;
import cm.ftg.tontine.member.entity.Member;
import java.time.Instant;
import java.util.UUID;

public record MemberRegistryDto(
        UUID id,
        UUID userId,
        String matricule,
        String firstName,
        String lastName,
        String phone,
        String email,
        MemberStatus status,
        Instant joinedAt
) {

    public static MemberRegistryDto from(Member m) {
        return new MemberRegistryDto(
                m.getId(), m.getUserId(), m.getMatricule(),
                m.getFirstName(), m.getLastName(),
                m.getPhone(), m.getEmail(),
                m.getStatus(), m.getJoinedAt());
    }
}
