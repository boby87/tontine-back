package cm.ftg.tontine.president.vote.repository;

import cm.ftg.tontine.president.vote.entity.VoteOption;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteOptionRepository extends JpaRepository<VoteOption, UUID> {

    List<VoteOption> findByVoteIdOrderByDisplayOrderAsc(UUID voteId);
}
