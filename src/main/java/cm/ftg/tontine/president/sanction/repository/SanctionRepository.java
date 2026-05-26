package cm.ftg.tontine.president.sanction.repository;

import cm.ftg.tontine.president.sanction.entity.Sanction;
import cm.ftg.tontine.president.sanction.enums.SanctionStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SanctionRepository extends JpaRepository<Sanction, UUID> {

    List<Sanction> findByTontineIdAndStatusInOrderByIssuedAtDesc(UUID tontineId, List<SanctionStatus> statuses);

    List<Sanction> findByTontineIdAndStatusOrderByIssuedAtDesc(UUID tontineId, SanctionStatus status);

    List<Sanction> findByTontineIdOrderByIssuedAtDesc(UUID tontineId);

    List<Sanction> findByTontineIdAndSessionIdOrderByIssuedAtDesc(UUID tontineId, UUID sessionId);

    List<Sanction> findByTontineIdAndStatusAndSessionIdOrderByIssuedAtDesc(UUID tontineId, SanctionStatus status, UUID sessionId);

    List<Sanction> findByTontineIdAndAutoDetectedTrueAndStatusOrderByIssuedAtDesc(UUID tontineId, SanctionStatus status);

    Optional<Sanction> findFirstByTontineIdAndSessionIdAndMemberIdAndAutoDetectedTrueAndStatus(
            UUID tontineId, UUID sessionId, UUID memberId, SanctionStatus status);

    long countByTontineIdAndStatus(UUID tontineId, SanctionStatus status);

    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM Sanction s WHERE s.tontineId = :tontineId AND s.status = :status")
    BigDecimal sumAmountByTontineIdAndStatus(@Param("tontineId") UUID tontineId,
                                             @Param("status") SanctionStatus status);

    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM Sanction s "
            + "WHERE s.sessionId = :sessionId AND s.status IN :statuses")
    BigDecimal sumAmountBySessionAndStatuses(@Param("sessionId") UUID sessionId,
                                              @Param("statuses") List<SanctionStatus> statuses);
}
