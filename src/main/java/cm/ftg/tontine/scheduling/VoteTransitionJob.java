package cm.ftg.tontine.scheduling;

import cm.ftg.tontine.member.vote.service.VoterEligibilityChecker;
import cm.ftg.tontine.president.vote.dto.VoteDto;
import cm.ftg.tontine.president.vote.dto.VoteOptionDto;
import cm.ftg.tontine.president.vote.entity.Vote;
import cm.ftg.tontine.president.vote.enums.VoteStatus;
import cm.ftg.tontine.president.vote.repository.VoteOptionRepository;
import cm.ftg.tontine.president.vote.repository.VoteRepository;
import cm.ftg.tontine.president.vote.service.VoteService;
import cm.ftg.tontine.realtime.RealtimeEventPublisher;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transitions automatiques des votes :
 *  - DRAFT -> OPEN lorsque {@code opensAt} est atteint (recalcule {@code totalVoters}).
 *  - OPEN -> CLOSED lorsque {@code closesAt} est depasse (cloture systeme).
 */
@Component
@ConditionalOnProperty(name = "app.scheduling.vote-transition.enabled", havingValue = "true", matchIfMissing = true)
public class VoteTransitionJob {

    private static final Logger log = LoggerFactory.getLogger(VoteTransitionJob.class);

    private final VoteRepository voteRepository;
    private final VoteOptionRepository optionRepository;
    private final VoteService voteService;
    private final VoterEligibilityChecker eligibilityChecker;
    private final RealtimeEventPublisher realtime;

    public VoteTransitionJob(VoteRepository voteRepository,
                             VoteOptionRepository optionRepository,
                             VoteService voteService,
                             VoterEligibilityChecker eligibilityChecker,
                             RealtimeEventPublisher realtime) {
        this.voteRepository = voteRepository;
        this.optionRepository = optionRepository;
        this.voteService = voteService;
        this.eligibilityChecker = eligibilityChecker;
        this.realtime = realtime;
    }

    @Scheduled(fixedDelayString = "${app.scheduling.vote-transition.fixed-delay-ms:60000}")
    @Transactional
    public void transitionVotes() {
        Instant now = Instant.now();

        List<Vote> toOpen = voteRepository.findByStatusAndOpensAtBefore(VoteStatus.DRAFT, now);
        for (Vote v : toOpen) {
            v.setStatus(VoteStatus.OPEN);
            // Recalcule le nombre de votants eligibles au passage a l'ouverture (quorum).
            v.setTotalVoters(eligibilityChecker.countEligible(v));
            Vote saved = voteRepository.save(v);
            List<VoteOptionDto> options = optionRepository.findByVoteIdOrderByDisplayOrderAsc(saved.getId())
                    .stream().map(VoteOptionDto::from).toList();
            realtime.toVote(saved.getTontineId(), saved.getId(), "vote.opened", VoteDto.from(saved, options));
        }

        List<Vote> toClose = voteRepository.findByStatusAndClosesAtBefore(VoteStatus.OPEN, now);
        for (Vote v : toClose) {
            voteService.closeBySystem(v.getId(), v.getTontineId());
        }

        if (!toOpen.isEmpty() || !toClose.isEmpty()) {
            log.info("[VoteTransitionJob] {} ouverts, {} clotures", toOpen.size(), toClose.size());
        }
    }
}
