package cm.ftg.tontine.service;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Requête de création d'une tontine.
 * Validée côté contrôleur via @Valid.
 */
public record CreateTontineRequest(

    @NotBlank
    String nom,

    String description,

    @NotNull @Positive @Digits(integer = 13, fraction = 2)
    BigDecimal montantCotisation,

    @NotNull @Positive @Digits(integer = 3, fraction = 2)
    BigDecimal tauxAmendeForfaitaireJour,

    @NotNull @Positive @Digits(integer = 3, fraction = 2)
    BigDecimal plafondAmendeEnPourcentage,

    @NotNull
    String contributionFrequency,

    @NotNull
    String distributionMode,

    @Positive
    Integer cycleSessionsCount,

    /** ID de l'utilisateur créateur (futur PRESIDENT) */
    @NotBlank
    String createdByUserId
) {}
