package cm.ftg.tontine.president.session.repository;

import cm.ftg.tontine.president.session.entity.AgendaItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgendaItemRepository extends JpaRepository<AgendaItem, UUID> {

    List<AgendaItem> findBySessionIdOrderByOrderIdxAsc(UUID sessionId);
}
