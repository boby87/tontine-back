package cm.ftg.tontine.president.cycleclose.repository;

import cm.ftg.tontine.president.cycleclose.entity.CycleCloseChecklistItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CycleCloseChecklistRepository extends JpaRepository<CycleCloseChecklistItem, UUID> {

    List<CycleCloseChecklistItem> findByCycleCloseIdOrderByItemKeyAsc(UUID cycleCloseId);

    Optional<CycleCloseChecklistItem> findByCycleCloseIdAndItemKey(UUID cycleCloseId, String itemKey);
}
