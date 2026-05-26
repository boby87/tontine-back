package cm.ftg.tontine.censor.attendance.repository;

import cm.ftg.tontine.censor.attendance.entity.AttendanceModificationRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendanceModificationRequestRepository
        extends JpaRepository<AttendanceModificationRequest, UUID> {

    List<AttendanceModificationRequest> findByTontineIdOrderByRequestedAtDesc(UUID tontineId);

    long countByTontineIdAndStatus(UUID tontineId,
            cm.ftg.tontine.censor.attendance.enums.AttendanceModificationStatus status);
}
