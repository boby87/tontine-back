package cm.ftg.tontine.controller;

import cm.ftg.tontine.service.ContributionResult;

import org.springframework.stereotype.Component;

@Component
public class ContributionMapper {

    public ContributionResponse toResponse(ContributionResult result) {
        return new ContributionResponse(
            result.cotisationId(),
            result.cleIdempotence(),
            result.membreId(),
            result.seanceId(),
            result.montantCotisation(),
            result.statut().name(),
            result.montantAmende(),
            result.joursRetard(),
            result.dateOperation()
        );
    }
}
