package cm.ftg.tontine.auditor.balancereview.repository;

import cm.ftg.tontine.auditor.balancereview.entity.SessionBalanceReview;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionBalanceReviewRepository extends JpaRepository<SessionBalanceReview, UUID> {

    List<SessionBalanceReview> findByTontineIdOrderByReviewedAtDesc(UUID tontineId);

    boolean existsBySessionId(UUID sessionId);
}
