package cm.ftg.tontine.treasurer.cashbox.repository;

import cm.ftg.tontine.treasurer.cashbox.entity.CashMovement;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashMovementRepository extends JpaRepository<CashMovement, UUID> {

    List<CashMovement> findByCashBoxIdOrderByRecordedAtDesc(UUID cashBoxId, Pageable pageable);

    List<CashMovement> findTop10ByTontineIdOrderByRecordedAtDesc(UUID tontineId);

    List<CashMovement> findTop100ByTontineIdOrderByRecordedAtDesc(UUID tontineId);
}
