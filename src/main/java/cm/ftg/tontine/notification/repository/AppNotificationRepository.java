package cm.ftg.tontine.notification.repository;

import cm.ftg.tontine.notification.entity.AppNotification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AppNotificationRepository extends JpaRepository<AppNotification, UUID> {

    Page<AppNotification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<AppNotification> findByUserIdAndTontineIdOrderByCreatedAtDesc(
            UUID userId, UUID tontineId, Pageable pageable);

    Page<AppNotification> findByUserIdAndReadFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndReadFalse(UUID userId);

    List<AppNotification> findByUserIdAndReadFalse(UUID userId);

    @Modifying
    @Query("UPDATE AppNotification n SET n.read = true, n.readAt = :now "
            + "WHERE n.userId = :userId AND n.read = false")
    int markAllReadForUser(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM AppNotification n WHERE n.userId = :userId AND n.read = true")
    int deleteAllReadForUser(@Param("userId") UUID userId);
}
