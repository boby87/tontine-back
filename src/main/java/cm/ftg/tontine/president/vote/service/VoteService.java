package cm.ftg.tontine.president.vote.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auth.entity.UserEntity;
import cm.ftg.tontine.auth.repository.UserRepository;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.president.security.PresidentAccessChecker;
import cm.ftg.tontine.realtime.RealtimeEventPublisher;
import cm.ftg.tontine.president.vote.dto.CreateVoteRequest;
import cm.ftg.tontine.president.vote.dto.VoteDto;
import cm.ftg.tontine.president.vote.dto.VoteOptionDto;
import cm.ftg.tontine.president.vote.entity.Vote;
import cm.ftg.tontine.president.vote.entity.VoteOption;
import cm.ftg.tontine.president.vote.enums.VoteStatus;
import cm.ftg.tontine.president.vote.repository.VoteOptionRepository;
import cm.ftg.tontine.president.vote.repository.VoteRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoteService {

    private final VoteRepository voteRepository;
    private final VoteOptionRepository optionRepository;
    private final PresidentAccessChecker accessChecker;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final RealtimeEventPublisher realtime;

    public VoteService(VoteRepository voteRepository,
                       VoteOptionRepository optionRepository,
                       PresidentAccessChecker accessChecker,
                       AuditService auditService,
                       UserRepository userRepository,
                       RealtimeEventPublisher realtime) {
        this.voteRepository = voteRepository;
        this.optionRepository = optionRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.realtime = realtime;
    }

    @Transactional(readOnly = true)
    public List<VoteDto> list(UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        return voteRepository.findByTontineIdOrderByCreatedAtDesc(tontineId).stream()
                .map(v -> VoteDto.from(v, loadOptions(v.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public VoteDto findById(UUID id, UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        Vote v = loadInTontine(id, tontineId);
        return VoteDto.from(v, loadOptions(v.getId()));
    }

    @Transactional
    public VoteDto create(UUID tontineId, UUID userId, CreateVoteRequest req) {
        accessChecker.requirePresident(userId, tontineId);
        if (req.options().size() < 2) {
            throw new ApiException("VOTE_INVALID_OPTIONS",
                    "Au moins 2 options sont requises", HttpStatus.valueOf(422));
        }
        if (!req.opensAt().isBefore(req.closesAt())) {
            throw new ApiException("VOTE_INVALID_WINDOW",
                    "opensAt doit etre anterieur a closesAt", HttpStatus.valueOf(422));
        }
        UserEntity author = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND",
                        "Utilisateur introuvable", HttpStatus.NOT_FOUND));
        Vote v = new Vote();
        v.setTontineId(tontineId);
        v.setQuestion(req.question());
        v.setDescription(req.description());
        v.setAnonymous(req.isAnonymous());
        v.setHideResultsUntilClose(req.hideResultsUntilClose());
        v.setScope(req.scope());
        v.setAudience(req.audience());
        v.setOpensAt(req.opensAt());
        v.setClosesAt(req.closesAt());
        v.setQuorumPercent(req.quorumPercent());
        v.setCreatedByUserId(userId);
        v.setCreatedByFullName(buildFullName(author));
        v.setTotalVoters(0);
        v.setTotalVoted(0);
        v.setStatus(!req.opensAt().isAfter(Instant.now()) ? VoteStatus.OPEN : VoteStatus.DRAFT);
        Vote saved = voteRepository.save(v);

        List<VoteOption> options = new ArrayList<>();
        int order = 0;
        for (String label : req.options()) {
            VoteOption opt = new VoteOption();
            opt.setVoteId(saved.getId());
            opt.setLabel(label);
            opt.setDisplayOrder(order++);
            opt.setCount(0);
            options.add(opt);
        }
        List<VoteOption> persistedOptions = optionRepository.saveAll(options);
        auditService.record(userId, "VOTE_CREATE", "Vote", saved.getId().toString(), tontineId,
                "{\"scope\":\"" + req.scope().name() + "\",\"audience\":\"" + req.audience().name() + "\"}");
        VoteDto dto = VoteDto.from(saved, persistedOptions.stream()
                .sorted(Comparator.comparingInt(VoteOption::getDisplayOrder))
                .map(VoteOptionDto::from)
                .toList());
        realtime.toVote(tontineId, saved.getId(), "vote.created", dto);
        return dto;
    }

    @Transactional
    public VoteDto close(UUID id, UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        Vote v = loadInTontine(id, tontineId);
        if (v.getStatus() != VoteStatus.OPEN) {
            throw new ApiException("VOTE_INVALID_STATE",
                    "Seul un vote OPEN peut etre cloture", HttpStatus.CONFLICT);
        }
        v.setStatus(VoteStatus.CLOSED);
        List<VoteOption> options = optionRepository.findByVoteIdOrderByDisplayOrderAsc(id);
        v.setPassed(computePassed(options));
        Vote saved = voteRepository.save(v);
        auditService.record(userId, "VOTE_CLOSE", "Vote", id.toString(), tontineId, null);
        VoteDto dto = VoteDto.from(saved, options.stream().map(VoteOptionDto::from).toList());
        realtime.toVote(tontineId, saved.getId(), "vote.closed", dto);
        return dto;
    }

    private Boolean computePassed(List<VoteOption> options) {
        if (options == null || options.isEmpty()) {
            return Boolean.FALSE;
        }
        VoteOption first = options.get(0);
        long firstCount = first.getCount();
        for (int i = 1; i < options.size(); i++) {
            if (options.get(i).getCount() >= firstCount) {
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    private List<VoteOptionDto> loadOptions(UUID voteId) {
        return optionRepository.findByVoteIdOrderByDisplayOrderAsc(voteId).stream()
                .map(VoteOptionDto::from)
                .toList();
    }

    private Vote loadInTontine(UUID id, UUID tontineId) {
        Vote v = voteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vote", id));
        if (!v.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        return v;
    }

    private String buildFullName(UserEntity u) {
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last = u.getLastName() == null ? "" : u.getLastName();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? u.getEmail() : full;
    }
}
