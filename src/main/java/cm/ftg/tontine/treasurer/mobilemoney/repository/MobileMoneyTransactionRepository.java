package cm.ftg.tontine.treasurer.mobilemoney.repository;

import cm.ftg.tontine.treasurer.cashbox.enums.MovementDirection;
import cm.ftg.tontine.treasurer.mobilemoney.entity.MobileMoneyTransaction;
import cm.ftg.tontine.treasurer.mobilemoney.enums.MobileMoneyStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MobileMoneyTransactionRepository extends JpaRepository<MobileMoneyTransaction, UUID> {

    List<MobileMoneyTransaction> findByTontineIdOrderByReceivedAtDesc(UUID tontineId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM MobileMoneyTransaction t "
            + "WHERE t.tontineId = :tontineId AND t.direction = :direction")
    BigDecimal sumAmountByDirection(@Param("tontineId") UUID tontineId,
                                     @Param("direction") MovementDirection direction);

    long countByTontineIdAndStatus(UUID tontineId, MobileMoneyStatus status);

    @Query("SELECT COUNT(t) FROM MobileMoneyTransaction t "
            + "WHERE t.tontineId = :tontineId AND t.matchedMemberId IS NOT NULL")
    long countMatched(@Param("tontineId") UUID tontineId);

    @Query("SELECT COUNT(t) FROM MobileMoneyTransaction t "
            + "WHERE t.tontineId = :tontineId AND t.matchedMemberId IS NULL")
    long countUnmatched(@Param("tontineId") UUID tontineId);
}
