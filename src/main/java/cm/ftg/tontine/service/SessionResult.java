package cm.ftg.tontine.service;

import cm.ftg.tontine.domain.SessionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Résultat immuable de la planification d'une séance.
 */
public record SessionResult(
    Long sessionId,
    Long tontineId,
    int sessionNumber,
    LocalDate scheduledDate,
    LocalTime startTime,
    LocalTime endTimePlanned,
    String locationAddress,
    String agenda,
    SessionStatus status,
    String createdByUserId,
    LocalDateTime createdAt
) {}
