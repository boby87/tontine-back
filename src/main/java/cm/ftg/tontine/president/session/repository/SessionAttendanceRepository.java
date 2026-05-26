package cm.ftg.tontine.president.session.repository;

import cm.ftg.tontine.president.session.entity.SessionAttendanceEntry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionAttendanceRepository extends JpaRepository<SessionAttendanceEntry, UUID> {

    List<SessionAttendanceEntry> findBySessionIdOrderByFullNameAsc(UUID sessionId);

    List<SessionAttendanceEntry> findBySessionId(UUID sessionId);

    Optional<SessionAttendanceEntry> findBySessionIdAndMemberId(UUID sessionId, UUID memberId);
}
