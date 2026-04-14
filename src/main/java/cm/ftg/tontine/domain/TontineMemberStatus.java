package cm.ftg.tontine.domain;

/**
 * Statut d'un membre au sein d'une tontine — CDC Section 7.1 TontineMember.
 * Nommage francophone conforme aux traditions de gestion communautaire camerounaise.
 */
public enum TontineMemberStatus {
    /** Membre actif et à jour de ses cotisations */
    ACTIF,
    /** Demande d'adhésion en attente d'approbation par le bureau */
    EN_ATTENTE,
    /** Membre exclu de la tontine par décision du bureau */
    EXCLU
}
