package cm.ftg.tontine.service;

/**
 * Résultat immuable de l'authentification — token + infos publiques.
 * Aucune donnée sensible (security-rules §4).
 */
public record AuthResult(
    String accessToken,
    String userId,
    String email,
    String role
) {}

