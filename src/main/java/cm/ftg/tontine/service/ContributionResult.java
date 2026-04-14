package cm.ftg.tontine.service;

import cm.ftg.tontine.domain.StatutCotisation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ContributionResult(
    Long cotisationId,
    String cleIdempotence,
    Long membreId,
    Long seanceId,
    BigDecimal montantCotisation,
    StatutCotisation statut,
    BigDecimal montantAmende,
    long joursRetard,
    LocalDateTime dateOperation
) {}
