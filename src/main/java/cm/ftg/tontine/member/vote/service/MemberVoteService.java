package cm.ftg.tontine.member.vote.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.member.vote.dto.MemberVoteDto;
import cm.ftg.tontine.member.vote.dto.VoteBallotDto;
import cm.ftg.tontine.member.vote.entity.VoteBallot;
import cm.ftg.tontine.member.vote.repository.VoteBallotRepository;
import cm.ftg.tontine.president.vote.dto.VoteOptionDto;
import cm.ftg.tontine.president.vote.entity.Vote;
import cm.ftg.tontine.president.vote.entity.VoteOption;
import cm.ftg.tontine.president.vote.enums.VoteStatus;
import cm.ftg.tontine.president.vote.repository.VoteOptionRepository;
import cm.ftg.tontine.president.vote.repository.VoteRepository;
import cm.ftg.tontine.realtime.RealtimeEventPublisher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logique cote membre : consultation et soumission des votes (motions) crees
 * par le President. Reutilise les entites {@code Vote} / {@code VoteOption} du
 * module {@code president.vote.*}.
 */
@Service
public class MemberVoteService {

    private static final HttpStatus UNPROCESSABLE = HttpStatus.valueOf(422);

    private final VoteRepository voteRepository;
    private final VoteOptionRepository optionRepository;
    private final VoteBallotRepository ballotRepository;
    private final MemberRepository memberRepository;
    private final VoterEligibilityChecker eligibilityChecker;
    private final AuditService auditService;
    private final RealtimeEventPublisher realtime;
    private final String anonymousSecret;

    public MemberVoteService(VoteRepository voteRepository,
                             VoteOptionRepository optionRepository,
                             VoteBallotRepository ballotRepository,
                             MemberRepository memberRepository,
                             VoterEligibilityChecker eligibilityChecker,
                             AuditService auditService,
                             RealtimeEventPublisher realtime,
                             @Value("${app.vote.anonymous-secret}") String anonymousSecret) {
        this.voteRepository = voteRepository;
        this.optionRepository = optionRepository;
        this.ballotRepository = ballotRepository;
        this.memberRepository = memberRepository;
        this.eligibilityChecker = eligibilityChecker;
        this.auditService = auditService;
        this.realtime = realtime;
        this.anonymousSecret = anonymousSecret;
    }

    @Transactional(readOnly = true)
    public List<MemberVoteDto> listForMember(UUID tontineId, UUID userId, VoteStatus status) {
        Member member = requireMember(userId, tontineId);
        List<Vote> votes = (status != null)
                ? voteRepository.findByTontineIdAndStatusOrderByCreatedAtDesc(tontineId, status)
                : voteRepository.findByTontineIdOrderByCreatedAtDesc(tontineId);
        return votes.stream()
                // DRAFT toujours masque au membre, meme si status=DRAFT explicitement demande.
                .filter(v -> v.getStatus() != VoteStatus.DRAFT)
                .filter(v -> eligibilityChecker.isEligible(userId, tontineId, v))
                .map(v -> toDto(v, member))
                .toList();
    }

    @Transactional(readOnly = true)
    public MemberVoteDto findForMember(UUID id, UUID tontineId, UUID userId) {
        Vote v = loadInTontine(id, tontineId);
        Member member = requireEligible(userId, tontineId, v);
        return toDto(v, member);
    }

    @Transactional(readOnly = true)
    public boolean hasVoted(UUID id, UUID tontineId, UUID userId) {
        Vote v = loadInTontine(id, tontineId);
        Member member = requireMember(userId, tontineId);
        return ballotRepository.existsByVoteIdAndVoterKey(v.getId(), voterKey(v, member.getId()));
    }

    @Transactional
    public VoteBallotDto cast(UUID id, UUID tontineId, UUID userId, UUID optionId) {
        Vote v = loadInTontine(id, tontineId);
        Member member = requireEligible(userId, tontineId, v);

        // 3.3 — Fenetre temporelle
        if (v.getStatus() != VoteStatus.OPEN) {
            throw new ApiException("VOTE_NOT_OPEN",
                    "Ce vote n'est pas ouvert aux votants", UNPROCESSABLE);
        }
        Instant now = Instant.now();
        if (now.isBefore(v.getOpensAt()) || now.isAfter(v.getClosesAt())) {
            throw new ApiException("VOTE_OUTSIDE_WINDOW",
                    "La periode de vote n'est pas active", UNPROCESSABLE);
        }

        // Validation de l'option
        VoteOption option = optionRepository.findById(optionId)
                .orElseThrow(() -> new ApiException("OPTION_NOT_FOUND",
                        "Option introuvable", HttpStatus.NOT_FOUND));
        if (!option.getVoteId().equals(v.getId())) {
            throw new ApiException("VOTE_OPTION_MISMATCH",
                    "L'option ne correspond pas a ce vote", UNPROCESSABLE);
        }

        // 3.2 — Unicite du vote
        String voterKey = voterKey(v, member.getId());
        if (ballotRepository.existsByVoteIdAndVoterKey(v.getId(), voterKey)) {
            throw new ApiException("VOTE_ALREADY_CAST",
                    "Vous avez deja vote pour cette motion", HttpStatus.CONFLICT);
        }

        // 3.4 / 3.5 — Insertion du bulletin (anonymise si besoin)
        VoteBallot ballot = new VoteBallot();
        ballot.setVoteId(v.getId());
        ballot.setOptionId(optionId);
        ballot.setVoterKey(voterKey);
        ballot.setMemberId(v.isAnonymous() ? null : member.getId());
        try {
            ballotRepository.saveAndFlush(ballot);
        } catch (DataIntegrityViolationException ex) {
            // Course concurrente : la contrainte UNIQUE (vote_id, voter_key) a saute.
            throw new ApiException("VOTE_ALREADY_CAST",
                    "Vous avez deja vote pour cette motion", HttpStatus.CONFLICT);
        }

        // Compteurs (meme transaction)
        int updatedOption = optionRepository.incrementCount(optionId, v.getId());
        if (updatedOption != 1) {
            throw new ApiException("VOTE_OPTION_MISMATCH",
                    "L'option ne correspond pas a ce vote", UNPROCESSABLE);
        }
        voteRepository.incrementTotalVoted(v.getId());

        // Evenement temps reel
        List<VoteOption> freshOptions = optionRepository.findByVoteIdOrderByDisplayOrderAsc(v.getId());
        Map<String, Long> optionCounts = new LinkedHashMap<>();
        freshOptions.forEach(o -> optionCounts.put(o.getId().toString(), o.getCount()));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("voteId", v.getId());
        payload.put("totalVoted", v.getTotalVoted() + 1);
        payload.put("optionCounts", optionCounts);
        realtime.toVote(tontineId, v.getId(), "vote.ballot-cast", payload);

        // Audit
        auditService.record(userId, "VOTE_CAST", "Vote", v.getId().toString(), tontineId,
                "{\"optionId\":\"" + optionId + "\"}");

        return new VoteBallotDto(v.getId(), optionId, option.getLabel(), ballot.getCastAt());
    }

    private MemberVoteDto toDto(Vote v, Member member) {
        List<VoteOption> options = optionRepository.findByVoteIdOrderByDisplayOrderAsc(v.getId());
        boolean mask = v.isHideResultsUntilClose() && v.getStatus() != VoteStatus.CLOSED;
        List<VoteOptionDto> optionDtos = options.stream()
                .map(o -> new VoteOptionDto(o.getId(), o.getLabel(), mask ? 0L : o.getCount()))
                .toList();
        int totalVoted = mask ? 0 : v.getTotalVoted();
        boolean hasVoted = ballotRepository.existsByVoteIdAndVoterKey(v.getId(), voterKey(v, member.getId()));
        return new MemberVoteDto(
                v.getId(), v.getTontineId(), v.getQuestion(), v.getDescription(), optionDtos,
                v.isAnonymous(), v.isHideResultsUntilClose(), v.getScope(), v.getAudience(),
                v.getStatus(), v.getOpensAt(), v.getClosesAt(), v.getCreatedAt(),
                v.getTotalVoters(), totalVoted, v.getQuorumPercent(), v.getPassed(), hasVoted);
    }

    private Vote loadInTontine(UUID id, UUID tontineId) {
        Vote v = voteRepository.findById(id)
                .orElseThrow(() -> new ApiException("VOTE_NOT_FOUND",
                        "Vote introuvable", HttpStatus.NOT_FOUND));
        if (!v.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        return v;
    }

    private Member requireMember(UUID userId, UUID tontineId) {
        return memberRepository.findByUserIdAndTontineId(userId, tontineId)
                .orElseThrow(() -> new ApiException("FORBIDDEN",
                        "Tontine non concordante", HttpStatus.FORBIDDEN));
    }

    private Member requireEligible(UUID userId, UUID tontineId, Vote vote) {
        Member member = requireMember(userId, tontineId);
        if (!eligibilityChecker.isEligible(userId, tontineId, vote)) {
            throw new ApiException("VOTE_NOT_ELIGIBLE",
                    "Vous n'etes pas eligible a participer a ce vote", HttpStatus.FORBIDDEN);
        }
        return member;
    }

    /**
     * Cle d'unicite du votant :
     *  - non-anonyme : memberId
     *  - anonyme : SHA-256(voteId + memberId + serverSecret) — bloque le re-vote
     *    sans permettre de retrouver qui a vote quoi.
     */
    private String voterKey(Vote vote, UUID memberId) {
        if (!vote.isAnonymous()) {
            return memberId.toString();
        }
        String material = vote.getId().toString() + memberId.toString() + anonymousSecret;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(material.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponible", ex);
        }
    }
}
