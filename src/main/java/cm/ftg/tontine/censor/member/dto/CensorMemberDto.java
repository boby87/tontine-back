package cm.ftg.tontine.censor.member.dto;

import cm.ftg.tontine.common.enums.MemberStatus;
import cm.ftg.tontine.member.entity.Member;
import java.math.BigDecimal;
import java.util.UUID;

public record CensorMemberDto(
        UUID id,
        String matricule,
        String firstName,
        String lastName,
        String phone,
        MemberStatus status,
        BigDecimal totalArrears
) {

    public static CensorMemberDto from(Member m) {
        return new CensorMemberDto(
                m.getId(),
                m.getMatricule(),
                m.getFirstName(),
                m.getLastName(),
                m.getPhone(),
                m.getStatus(),
                m.getTotalArrears());
    }
}
