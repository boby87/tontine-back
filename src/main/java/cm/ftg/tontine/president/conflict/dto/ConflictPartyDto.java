package cm.ftg.tontine.president.conflict.dto;

import cm.ftg.tontine.president.conflict.entity.ConflictParty;
import cm.ftg.tontine.president.conflict.enums.ConflictPartyRole;
import java.util.UUID;

public record ConflictPartyDto(
        UUID memberId,
        String fullName,
        ConflictPartyRole role
) {

    public static ConflictPartyDto from(ConflictParty p) {
        return new ConflictPartyDto(p.getMemberId(), p.getFullName(), p.getRole());
    }
}
