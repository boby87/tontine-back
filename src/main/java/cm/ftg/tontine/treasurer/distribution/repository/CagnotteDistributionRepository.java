package cm.ftg.tontine.treasurer.distribution.repository;

import cm.ftg.tontine.treasurer.distribution.entity.CagnotteDistribution;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CagnotteDistributionRepository extends JpaRepository<CagnotteDistribution, UUID> {

    List<CagnotteDistribution> findByTontineIdOrderByCreatedAtDesc(UUID tontineId);

    @Query("SELECT COALESCE(SUM(d.netAmount), 0) FROM CagnotteDistribution d "
            + "WHERE d.tontineId = :tontineId")
    BigDecimal sumNetAmountByTontine(@Param("tontineId") UUID tontineId);

    @Query("SELECT d.beneficiaryMemberId FROM CagnotteDistribution d WHERE d.cycleId = :cycleId")
    Set<UUID> findBeneficiaryMemberIdsByCycleId(@Param("cycleId") UUID cycleId);
}
