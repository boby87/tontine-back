package cm.ftg.tontine.domain;

/**
 * Types de sanctions applicables — CDC Section 4.3.1.
 * Nommage francophone conforme aux traditions de gestion communautaire camerounaise.
 */
public enum SanctionType {
    /** Arrivée après l'heure officielle */
    RETARD,
    /** Absence sans justificatif valide */
    ABSENCE_NON_JUSTIFIEE,
    /** Absence avec justificatif accepté (montant peut être 0) */
    ABSENCE_JUSTIFIEE,
    /** Perturbation de la réunion */
    TROUBLE_A_LORDRE,
    /** Cotisation non payée à temps */
    NON_PAIEMENT,
    /** Type personnalisable */
    AUTRE
}
