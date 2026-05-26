package cm.ftg.tontine.president.vote.dto;

import cm.ftg.tontine.president.vote.entity.VoteOption;
import java.util.UUID;

public record VoteOptionDto(
        UUID id,
        String label,
        long count
) {

    public static VoteOptionDto from(VoteOption o) {
        return new VoteOptionDto(o.getId(), o.getLabel(), o.getCount());
    }
}
