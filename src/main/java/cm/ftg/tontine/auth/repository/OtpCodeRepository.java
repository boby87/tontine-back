package cm.ftg.tontine.auth.repository;

import cm.ftg.tontine.auth.entity.OtpCode;
import cm.ftg.tontine.common.enums.OtpPurpose;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {

    Optional<OtpCode> findTopByIdentifierAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
            String identifier, OtpPurpose purpose);

    @Modifying
    @Query("DELETE FROM OtpCode o WHERE o.expiresAt < :cutoff")
    int deleteByExpiresAtBefore(@Param("cutoff") Instant cutoff);
}
