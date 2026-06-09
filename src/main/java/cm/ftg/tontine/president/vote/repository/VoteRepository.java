package cm.ftg.tontine.president.vote.repository;

import cm.ftg.tontine.president.vote.entity.Vote;
import cm.ftg.tontine.president.vote.enums.VoteStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteRepository extends JpaRepository<Vote, UUID> {

    List<Vote> findByTontineIdOrderByCreatedAtDesc(UUID tontineId);

    List<Vote> findByTontineIdAndStatusOrderByCreatedAtDesc(UUID tontineId, VoteStatus status);

    List<Vote> findByStatusAndOpensAtBefore(VoteStatus status, Instant moment);

    List<Vote> findByStatusAndClosesAtBefore(VoteStatus status, Instant moment);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Vote v set v.totalVoted = v.totalVoted + 1 where v.id = :voteId")
    int incrementTotalVoted(@Param("voteId") UUID voteId);
}
