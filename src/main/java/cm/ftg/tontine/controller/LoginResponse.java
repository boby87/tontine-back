package cm.ftg.tontine.controller;

/**
 * Réponse de connexion — contient le token JWT et les informations publiques de l'utilisateur.
 * Aucune donnée sensible (mot de passe, CNI, montant) — security-rules §4.
 */
public record LoginResponse(
    String accessToken,
    String tokenType,
    String userId,
    String email,
    String role
) {}

