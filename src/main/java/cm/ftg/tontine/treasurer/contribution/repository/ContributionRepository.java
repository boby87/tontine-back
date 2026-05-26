package cm.ftg.tontine.treasurer.contribution.repository;

import cm.ftg.tontine.treasurer.contribution.entity.Contribution;
import cm.ftg.tontine.treasurer.contribution.enums.ContributionStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContributionRepository extends JpaRepository<Contribution, UUID> {

    List<Contribution> findByTontineIdAndSessionIdOrderByMemberIdAsc(UUID tontineId, UUID sessionId);

    @Query("SELECT c FROM Contribution c WHERE c.tontineId = :tontineId "
            + "ORDER BY COALESCE(c.paidAt, c.createdAt) DESC")
    List<Contribution> findByTontineIdOrderByPaidAtDesc(@Param("tontineId") UUID tontineId);

    @Query("SELECT COALESCE(SUM(c.paidAmount), 0) FROM Contribution c "
            + "WHERE c.tontineId = :tontineId AND c.status IN :statuses")
    BigDecimal sumPaidAmountByTontineAndStatuses(@Param("tontineId") UUID tontineId,
                                                  @Param("statuses") List<ContributionStatus> statuses);

    Optional<Contribution> findBySessionIdAndMemberId(UUID sessionId, UUID memberId);

    @Query("SELECT COALESCE(SUM(c.paidAmount), 0) FROM Contribution c "
            + "WHERE c.sessionId = :sessionId AND c.status IN :statuses")
    BigDecimal sumPaidAmountBySessionAndStatuses(@Param("sessionId") UUID sessionId,
                                                  @Param("statuses") List<ContributionStatus> statuses);

    List<Contribution> findBySessionIdAndStatus(UUID sessionId, ContributionStatus status);
}
