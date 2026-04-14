package cm.ftg.tontine.domain;

/**
 * Rôles au sein d'une tontine — CDC Section 3.1 et 4.2.1.
 * Nommage francophone conforme aux traditions de gestion communautaire camerounaise.
 */
public enum TontineRole {
    /** Président du bureau — dirige les séances */
    PRESIDENT,
    /** Trésorier — gère la caisse et les cotisations */
    TRESORIER,
    /** Secrétaire — rédige les PV et tient le registre */
    SECRETAIRE,
    /** Censeur — contrôle la conformité des opérations */
    CENSEUR,
    /** Membre ordinaire */
    MEMBRE
}
