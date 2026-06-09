package cm.ftg.tontine.president.membership.invitation.repository;

import cm.ftg.tontine.president.membership.invitation.entity.MembershipInvitation;
import cm.ftg.tontine.president.membership.invitation.enums.InvitationStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipInvitationRepository extends JpaRepository<MembershipInvitation, UUID> {

    Optional<MembershipInvitation> findByToken(String token);

    List<MembershipInvitation> findByTontineIdOrderByInvitedAtDesc(UUID tontineId);

    long countByTontineIdAndStatus(UUID tontineId, InvitationStatus status);

    boolean existsByTontineIdAndCandidatePhoneAndStatusIn(UUID tontineId, String candidatePhone,
                                                          Collection<InvitationStatus> statuses);

    List<MembershipInvitation> findByStatusInAndExpiresAtBefore(Collection<InvitationStatus> statuses, Instant moment);
}
