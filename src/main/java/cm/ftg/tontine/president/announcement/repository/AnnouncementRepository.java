package cm.ftg.tontine.president.announcement.repository;

import cm.ftg.tontine.president.announcement.entity.Announcement;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    List<Announcement> findByTontineIdOrderByPublishedAtDesc(UUID tontineId);
}
