package cm.ftg.tontine.treasurer.cashbox.repository;

import cm.ftg.tontine.treasurer.cashbox.entity.CashBox;
import cm.ftg.tontine.treasurer.cashbox.enums.CashBoxType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashBoxRepository extends JpaRepository<CashBox, UUID> {

    List<CashBox> findByTontineIdOrderByTypeAscNameAsc(UUID tontineId);

    Optional<CashBox> findByTontineIdAndType(UUID tontineId, CashBoxType type);
}
