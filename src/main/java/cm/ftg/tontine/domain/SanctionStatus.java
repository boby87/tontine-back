package cm.ftg.tontine.domain;

/**
 * Statut d'une sanction — CDC Section 4.3.2.
 * Processus : IMPAYEE → PAYEE (ou CONTESTEE → ANNULEE par le bureau).
 */
public enum SanctionStatus {
    /** Sanction appliquée, en attente de paiement */
    IMPAYEE,
    /** Sanction réglée par le membre */
    PAYEE,
    /** Sanction contestée par le membre */
    CONTESTEE,
    /** Sanction annulée par décision du bureau */
    ANNULEE
}
