package cm.ftg.tontine.treasurer.cashbox.repository;

import cm.ftg.tontine.treasurer.cashbox.entity.CashBoxTransfer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashBoxTransferRepository extends JpaRepository<CashBoxTransfer, UUID> {

    List<CashBoxTransfer> findByTontineIdOrderByRequestedAtDesc(UUID tontineId);

    long countByTontineIdAndStatusIn(UUID tontineId,
            java.util.List<cm.ftg.tontine.treasurer.cashbox.enums.CashTransferStatus> statuses);
}
