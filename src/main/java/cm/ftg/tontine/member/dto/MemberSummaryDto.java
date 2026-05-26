package cm.ftg.tontine.member.dto;

import cm.ftg.tontine.tontine.dto.TontineDto;
import java.math.BigDecimal;
import java.util.UUID;

public record MemberSummaryDto(
        MemberDto member,
        TontineDto tontine,
        BigDecimal totalContributed,
        BigDecimal totalArrears,
        int activeLoans,
        Object nextSession,
        Integer tourPosition,
        UUID activeTontineId
) {
}
