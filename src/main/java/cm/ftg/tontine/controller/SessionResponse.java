package cm.ftg.tontine.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record SessionResponse(
    Long id,
    Long tontineId,
    int sessionNumber,
    LocalDate scheduledDate,
    LocalTime startTime,
    LocalTime endTimePlanned,
    String locationAddress,
    String agenda,
    String status,
    String createdByUserId,
    LocalDateTime createdAt
) {}
