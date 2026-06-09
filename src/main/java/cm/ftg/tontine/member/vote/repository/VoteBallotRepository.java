package cm.ftg.tontine.member.vote.repository;

import cm.ftg.tontine.member.vote.entity.VoteBallot;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteBallotRepository extends JpaRepository<VoteBallot, UUID> {

    boolean existsByVoteIdAndVoterKey(UUID voteId, String voterKey);
}
