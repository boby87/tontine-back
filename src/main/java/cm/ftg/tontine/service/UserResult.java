package cm.ftg.tontine.service;

import cm.ftg.tontine.domain.Region;
import cm.ftg.tontine.domain.UserRole;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Résultat immuable de l'inscription d'un utilisateur.
 * Jamais d'entité JPA exposée — uniquement ce record.
 */
public record UserResult(
    String userId,
    String email,
    String phone,
    String firstName,
    String lastName,
    LocalDate dateOfBirth,
    UserRole role,
    Region region,
    String cniNumber,
    LocalDateTime createdAt
) {}
