package cm.ftg.tontine.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction récente (cotisation) affichée dans le dashboard.
 * Ne jamais exposer de données sensibles non masquées.
 */
public record RecentTransaction(
    Long cotisationId,
    Long membreId,
    String membreNom,
    BigDecimal montant,
    String statut,
    LocalDateTime dateOperation
) {}

