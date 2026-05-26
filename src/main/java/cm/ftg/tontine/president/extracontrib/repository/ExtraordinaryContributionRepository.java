package cm.ftg.tontine.president.extracontrib.repository;

import cm.ftg.tontine.president.extracontrib.entity.ExtraordinaryContribution;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExtraordinaryContributionRepository extends JpaRepository<ExtraordinaryContribution, UUID> {

    List<ExtraordinaryContribution> findByTontineIdOrderByCreatedAtDesc(UUID tontineId);
}
