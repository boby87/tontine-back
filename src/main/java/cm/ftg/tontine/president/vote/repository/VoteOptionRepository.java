package cm.ftg.tontine.president.vote.repository;

import cm.ftg.tontine.president.vote.entity.VoteOption;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteOptionRepository extends JpaRepository<VoteOption, UUID> {

    List<VoteOption> findByVoteIdOrderByDisplayOrderAsc(UUID voteId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update VoteOption o set o.count = o.count + 1 where o.id = :optionId and o.voteId = :voteId")
    int incrementCount(@Param("optionId") UUID optionId, @Param("voteId") UUID voteId);
}
