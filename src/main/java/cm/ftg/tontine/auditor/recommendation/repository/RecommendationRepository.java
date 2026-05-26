package cm.ftg.tontine.auditor.recommendation.repository;

import cm.ftg.tontine.auditor.recommendation.entity.Recommendation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    List<Recommendation> findByTontineIdOrderByCreatedAtDesc(UUID tontineId);
}
