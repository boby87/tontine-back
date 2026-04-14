package cm.ftg.tontine.controller;

import cm.ftg.tontine.service.TontineResult;
import org.springframework.stereotype.Component;

@Component
public class TontineMapper {

    public TontineResponse toResponse(TontineResult result) {
        return new TontineResponse(
            result.tontineId(),
            result.nom(),
            result.description(),
            result.montantCotisation(),
            result.contributionFrequency() != null ? result.contributionFrequency().name() : null,
            result.distributionMode() != null ? result.distributionMode().name() : null,
            result.cycleSessionsCount(),
            result.createdByUserId(),
            result.presidentMemberId(),
            result.createdAt()
        );
    }
}
