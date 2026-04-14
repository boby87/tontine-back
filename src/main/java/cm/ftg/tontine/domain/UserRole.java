package cm.ftg.tontine.domain;

/**
 * Rôle global de l'utilisateur sur la plateforme.
 * Distinct de TontineRole qui représente le rôle au sein d'une tontine spécifique.
 * CDC Section 7.1 — User.
 */
public enum UserRole {
    ADMIN,
    PRESIDENT,
    TREASURER,
    SECRETARY,
    CENSOR,
    MEMBER
}
