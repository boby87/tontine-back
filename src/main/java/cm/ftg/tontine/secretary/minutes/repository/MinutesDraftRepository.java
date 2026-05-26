package cm.ftg.tontine.secretary.minutes.repository;

import cm.ftg.tontine.secretary.minutes.entity.MinutesDraft;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MinutesDraftRepository extends JpaRepository<MinutesDraft, UUID> {

    List<MinutesDraft> findByTontineIdOrderByCreatedAtDesc(UUID tontineId);

    long countByTontineIdAndStatusIn(UUID tontineId,
            java.util.List<cm.ftg.tontine.secretary.minutes.enums.MinutesStatus> statuses);
}
