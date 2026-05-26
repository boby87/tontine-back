package cm.ftg.tontine.secretary.rsvp.repository;

import cm.ftg.tontine.secretary.rsvp.entity.SessionRsvp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRsvpRepository extends JpaRepository<SessionRsvp, UUID> {

    List<SessionRsvp> findBySessionId(UUID sessionId);

    Optional<SessionRsvp> findBySessionIdAndMemberId(UUID sessionId, UUID memberId);
}
