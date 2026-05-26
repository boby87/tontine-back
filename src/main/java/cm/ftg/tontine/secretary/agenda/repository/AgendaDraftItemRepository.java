package cm.ftg.tontine.secretary.agenda.repository;

import cm.ftg.tontine.secretary.agenda.entity.AgendaDraftItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgendaDraftItemRepository extends JpaRepository<AgendaDraftItem, UUID> {

    List<AgendaDraftItem> findByAgendaDraftIdOrderByOrderIdxAsc(UUID agendaDraftId);
}
