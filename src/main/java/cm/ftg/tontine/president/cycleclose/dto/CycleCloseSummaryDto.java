package cm.ftg.tontine.president.cycleclose.dto;

import cm.ftg.tontine.president.cycleclose.entity.CycleClose;
import java.math.BigDecimal;

public record CycleCloseSummaryDto(
        BigDecimal totalCollected,
        BigDecimal totalDistributed,
        BigDecimal totalLoansOutstanding,
        BigDecimal totalSanctionsCollected,
        BigDecimal netResult,
        int membersRetained,
        int newMembersNextCycle
) {

    public static CycleCloseSummaryDto from(CycleClose c) {
        return new CycleCloseSummaryDto(
                c.getTotalCollected(), c.getTotalDistributed(), c.getTotalLoansOutstanding(),
                c.getTotalSanctionsCollected(), c.getNetResult(),
                c.getMembersRetained(), c.getNewMembersNextCycle());
    }
}
