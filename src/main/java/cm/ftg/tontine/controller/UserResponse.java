package cm.ftg.tontine.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserResponse(
    String userId,
    String email,
    String phone,
    String firstName,
    String lastName,
    LocalDate dateOfBirth,
    String role,
    String region,
    String cniNumber,
    LocalDateTime createdAt
) {}
