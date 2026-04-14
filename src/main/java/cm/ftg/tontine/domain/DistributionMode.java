package cm.ftg.tontine.domain;

/** Mode de distribution de la cagnotte — CDC Section 4.4.2 et 4.5.5 */
public enum DistributionMode {
    /** Tour de rôle : ordre défini à l'avance */
    ROTATION,
    /** Enchères : le plus offrant gagne */
    AUCTION,
    /** Tirage au sort transparent */
    LOTTERY
}
