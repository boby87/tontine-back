package cm.ftg.tontine.domain;

/** Statut d'une séance — CDC Section 7.1 Session */
public enum SessionStatus {
    /** Séance planifiée, pas encore commencée */
    SCHEDULED,
    /** Séance en cours */
    IN_PROGRESS,
    /** Séance terminée et clôturée */
    COMPLETED,
    /** Séance annulée */
    CANCELLED
}
