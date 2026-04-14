package cm.ftg.tontine.service;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Requête de planification d'une séance.
 * Validée côté contrôleur via @Valid.
 */
public record CreateSessionRequest(

    @NotNull
    Long tontineId,

    @NotNull
    LocalDate scheduledDate,

    LocalTime startTime,

    LocalTime endTimePlanned,

    String locationAddress,

    String agenda,

    /** ID de l'utilisateur qui crée la séance */
    @NotBlank
    String createdByUserId
) {}
