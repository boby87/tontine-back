package cm.ftg.tontine.repository;

import cm.ftg.tontine.domain.Cotisation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CotisationRepository extends JpaRepository<Cotisation, Long> {

    boolean existsByMembreIdAndSeanceId(Long membreId, Long seanceId);

    /**
     * Somme des montants des cotisations PAYEE et EN_RETARD pour une tontine donnée.
     * Retourne 0 si aucune cotisation n'existe.
     */
    @Query("SELECT COALESCE(SUM(c.montant), 0) FROM Cotisation c "
         + "WHERE c.seance.tontine.id = :tontineId "
         + "AND c.statut IN (cm.ftg.tontine.domain.StatutCotisation.PAYEE, "
         + "cm.ftg.tontine.domain.StatutCotisation.EN_RETARD)")
    BigDecimal sumMontantByTontineId(@Param("tontineId") Long tontineId);

    /**
     * Dernières cotisations d'une tontine, triées par date d'opération décroissante.
     * Utiliser {@code PageRequest.of(0, 5)} pour limiter à 5 résultats.
     */
    @Query("SELECT c FROM Cotisation c JOIN FETCH c.membre m "
         + "WHERE c.seance.tontine.id = :tontineId "
         + "ORDER BY c.dateOperation DESC")
    List<Cotisation> findTop5ByTontineIdOrderByDateOperationDesc(
        @Param("tontineId") Long tontineId, Pageable pageable);
}
