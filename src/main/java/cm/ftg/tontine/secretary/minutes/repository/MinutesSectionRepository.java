package cm.ftg.tontine.secretary.minutes.repository;

import cm.ftg.tontine.secretary.minutes.entity.MinutesSectionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MinutesSectionRepository extends JpaRepository<MinutesSectionEntity, UUID> {

    List<MinutesSectionEntity> findByMinutesDraftIdOrderByOrderIdxAsc(UUID minutesDraftId);

    void deleteByMinutesDraftId(UUID minutesDraftId);
}
