package cm.ftg.tontine.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ContributionResponse(
    Long id,
    String cleIdempotence,
    Long membreId,
    Long seanceId,
    BigDecimal montant,
    String statut,
    BigDecimal montantAmende,
    long joursRetard,
    LocalDateTime dateOperation
) {}
