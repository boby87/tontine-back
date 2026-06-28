package cm.ftg.tontine.treasurer.contribution.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.president.session.entity.Session;
import cm.ftg.tontine.president.session.repository.SessionRepository;
import cm.ftg.tontine.tontine.entity.Tontine;
import cm.ftg.tontine.tontine.repository.TontineRepository;
import cm.ftg.tontine.treasurer.cashbox.entity.CashBox;
import cm.ftg.tontine.treasurer.cashbox.enums.CashBoxType;
import cm.ftg.tontine.treasurer.cashbox.enums.CashMovementKind;
import cm.ftg.tontine.treasurer.cashbox.repository.CashBoxRepository;
import cm.ftg.tontine.treasurer.cashbox.service.CashBoxService;
import cm.ftg.tontine.treasurer.common.enums.PaymentMethod;
import cm.ftg.tontine.treasurer.contribution.dto.AdvancePaymentRequest;
import cm.ftg.tontine.treasurer.contribution.dto.ContributionDto;
import cm.ftg.tontine.treasurer.contribution.dto.PayContributionRequest;
import cm.ftg.tontine.treasurer.contribution.dto.RecordContributionRequest;
import cm.ftg.tontine.treasurer.contribution.entity.Contribution;
import cm.ftg.tontine.treasurer.contribution.enums.ContributionStatus;
import cm.ftg.tontine.treasurer.contribution.enums.ContributionType;
import cm.ftg.tontine.treasurer.contribution.repository.ContributionRepository;
import cm.ftg.tontine.treasurer.security.TreasurerAccessChecker;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContributionService {

    private final ContributionRepository contributionRepository;
    private final SessionRepository sessionRepository;
    private final MemberRepository memberRepository;
    private final TontineRepository tontineRepository;
    private final CashBoxRepository cashBoxRepository;
    private final CashBoxService cashBoxService;
    private final TreasurerAccessChecker accessChecker;
    private final AuditService auditService;

    public ContributionService(ContributionRepository contributionRepository,
                               SessionRepository sessionRepository,
                               MemberRepository memberRepository,
                               TontineRepository tontineRepository,
                               CashBoxRepository cashBoxRepository,
                               CashBoxService cashBoxService,
                               TreasurerAccessChecker accessChecker,
                               AuditService auditService) {
        this.contributionRepository = contributionRepository;
        this.sessionRepository = sessionRepository;
        this.memberRepository = memberRepository;
        this.tontineRepository = tontineRepository;
        this.cashBoxRepository = cashBoxRepository;
        this.cashBoxService = cashBoxService;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ContributionDto> listBySession(UUID tontineId, UUID sessionId, UUID userId, ContributionType type) {
        accessChecker.requireTreasurer(userId, tontineId);
        ensureSessionInTontine(sessionId, tontineId);
        List<Contribution> rows = type != null
                ? contributionRepository.findByTontineIdAndSessionIdAndContributionTypeOrderByMemberIdAsc(tontineId, sessionId, type)
                : contributionRepository.findByTontineIdAndSessionIdOrderByMemberIdAsc(tontineId, sessionId);
        Map<UUID, String> nameMap = memberRepository.findAllById(
                rows.stream().map(Contribution::getMemberId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Member::getId, this::fullName));
        return rows.stream()
                .map(c -> ContributionDto.from(c, nameMap.getOrDefault(c.getMemberId(), "")))
                .toList();
    }

    @Transactional
    public ContributionDto record(UUID tontineId, UUID userId, RecordContributionRequest req) {
        Member collector = accessChecker.requireTreasurer(userId, tontineId);
        ensureSessionInTontine(req.sessionId(), tontineId);

        Contribution c = contributionRepository
                .findBySessionIdAndMemberIdAndContributionType(req.sessionId(), req.memberId(), req.contributionType())
                .orElseGet(() -> {
                    Contribution nc = new Contribution();
                    nc.setTontineId(tontineId);
                    nc.setSessionId(req.sessionId());
                    nc.setMemberId(req.memberId());
                    nc.setContributionType(req.contributionType());
                    nc.setExpectedAmount(BigDecimal.ZERO);
                    return nc;
                });

        if (c.getStatus() == ContributionStatus.PAID || c.getStatus() == ContributionStatus.EXEMPTED) {
            throw new ApiException("CONTRIBUTION_ALREADY_SETTLED",
                    "Cette cotisation est deja reglee", HttpStatus.CONFLICT);
        }

        c.setPaidAmount(c.getPaidAmount().add(req.amount()));
        c.setExpectedAmount(c.getPaidAmount()); // montant libre : attendu = payé
        c.setStatus(ContributionStatus.PAID);
        c.setPaidAt(Instant.now());
        c.setPaymentMethod(req.paymentMethod());
        if (req.reference() != null) c.setReference(req.reference());
        if (req.note() != null) c.setNote(req.note());
        c.setCollectedByUserId(userId);

        Contribution saved = contributionRepository.save(c);

        creditPrincipal(tontineId, req.amount(), CashMovementKind.CONTRIBUTION_IN,
                "Cotisation " + req.contributionType() + " membre " + req.memberId(),
                saved.getId().toString(),
                fullName(collector));

        sessionRepository.findById(req.sessionId()).ifPresent(s -> {
            s.setTotalCollected(s.getTotalCollected().add(req.amount()));
            sessionRepository.save(s);
        });

        memberRepository.findById(req.memberId()).ifPresent(m -> {
            m.setTotalContributed(m.getTotalContributed().add(req.amount()));
            memberRepository.save(m);
        });

        auditService.record(userId, "CONTRIBUTION_RECORD", "Contribution", saved.getId().toString(),
                tontineId, "{\"type\":\"" + req.contributionType() + "\",\"amount\":\"***\"}");
        String memberName = memberRepository.findById(req.memberId()).map(this::fullName).orElse("");
        return ContributionDto.from(saved, memberName);
    }

    @Transactional
    public ContributionDto pay(UUID contributionId, UUID tontineId, UUID userId, PayContributionRequest req) {
        Member collector = accessChecker.requireTreasurer(userId, tontineId);
        Contribution c = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new ResourceNotFoundException("Contribution", contributionId));
        if (!c.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Cotisation hors de la tontine active", HttpStatus.FORBIDDEN);
        }
        if (c.getStatus() == ContributionStatus.PAID || c.getStatus() == ContributionStatus.EXEMPTED) {
            throw new ApiException("CONTRIBUTION_ALREADY_SETTLED",
                    "Cette cotisation est deja reglee", HttpStatus.CONFLICT);
        }
        BigDecimal amount = req.amount();
        BigDecimal remaining = c.getExpectedAmount().subtract(c.getPaidAmount());
        if (amount.compareTo(remaining) < 0) {
            Tontine tontine = tontineRepository.findById(tontineId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tontine", tontineId));
            if (!tontine.isPartialContributionAllowed()) {
                throw new ApiException("PARTIAL_NOT_ALLOWED",
                        "Les paiements partiels ne sont pas autorises pour cette tontine", HttpStatus.CONFLICT);
            }
            BigDecimal minAmount = tontine.getPartialContributionMin();
            if (minAmount != null && amount.compareTo(minAmount) < 0) {
                throw new ApiException("PARTIAL_BELOW_MINIMUM",
                        "Le montant minimum pour un paiement partiel est " + minAmount, HttpStatus.valueOf(422));
            }
        }
        c.setPaidAmount(c.getPaidAmount().add(amount));
        if (c.getPaidAmount().compareTo(c.getExpectedAmount()) >= 0) {
            c.setStatus(ContributionStatus.PAID);
            c.setPaidAt(Instant.now());
        } else {
            c.setStatus(ContributionStatus.PARTIAL);
        }
        c.setPaymentMethod(req.paymentMethod());
        if (req.reference() != null) {
            c.setReference(req.reference());
        }
        if (req.note() != null) {
            c.setNote(req.note());
        }
        c.setCollectedByUserId(userId);
        Contribution saved = contributionRepository.save(c);

        creditPrincipal(tontineId, amount, CashMovementKind.CONTRIBUTION_IN,
                "Cotisation #" + saved.getId() + " membre " + saved.getMemberId(),
                saved.getId().toString(),
                fullName(collector));

        Session session = sessionRepository.findById(saved.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session", saved.getSessionId()));
        session.setTotalCollected(session.getTotalCollected().add(amount));
        sessionRepository.save(session);

        memberRepository.findById(saved.getMemberId()).ifPresent(m -> {
            m.setTotalContributed(m.getTotalContributed().add(amount));
            memberRepository.save(m);
        });

        auditService.record(userId, "CONTRIBUTION_PAY", "Contribution", saved.getId().toString(),
                tontineId, "{\"amount\":\"***\"}");
        return ContributionDto.from(saved);
    }

    @Transactional
    public List<ContributionDto> advance(UUID tontineId, UUID userId, AdvancePaymentRequest req) {
        Member collector = accessChecker.requireTreasurer(userId, tontineId);
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new ResourceNotFoundException("Tontine", tontineId));
        BigDecimal defaultExpected = tontine.getContributionAmount();

        BigDecimal remaining = req.amount();
        BigDecimal applied = BigDecimal.ZERO;
        List<ContributionDto> result = new ArrayList<>();

        for (UUID sessionId : req.sessionIds()) {
            if (remaining.signum() <= 0) {
                break;
            }
            Session session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));
            if (!session.getTontineId().equals(tontineId)) {
                throw new ApiException("FORBIDDEN", "Seance hors de la tontine active", HttpStatus.FORBIDDEN);
            }
            Contribution c = contributionRepository.findBySessionIdAndMemberId(sessionId, req.memberId())
                    .orElseGet(() -> {
                        Contribution nc = new Contribution();
                        nc.setTontineId(tontineId);
                        nc.setSessionId(sessionId);
                        nc.setMemberId(req.memberId());
                        nc.setExpectedAmount(defaultExpected == null ? BigDecimal.ZERO : defaultExpected);
                        return nc;
                    });
            if (c.getStatus() == ContributionStatus.PAID || c.getStatus() == ContributionStatus.EXEMPTED) {
                result.add(ContributionDto.from(c.getId() == null ? contributionRepository.save(c) : c));
                continue;
            }
            BigDecimal due = c.getExpectedAmount().subtract(c.getPaidAmount());
            if (due.signum() <= 0) {
                c.setStatus(ContributionStatus.PAID);
                c.setPaidAt(Instant.now());
                Contribution saved = contributionRepository.save(c);
                result.add(ContributionDto.from(saved));
                continue;
            }
            BigDecimal toApply = due.min(remaining);
            c.setPaidAmount(c.getPaidAmount().add(toApply));
            if (c.getPaidAmount().compareTo(c.getExpectedAmount()) >= 0) {
                c.setStatus(ContributionStatus.PAID);
                c.setPaidAt(Instant.now());
            } else {
                c.setStatus(ContributionStatus.PARTIAL);
            }
            c.setPaymentMethod(req.paymentMethod());
            c.setCollectedByUserId(userId);
            Contribution saved = contributionRepository.save(c);

            session.setTotalCollected(session.getTotalCollected().add(toApply));
            sessionRepository.save(session);

            remaining = remaining.subtract(toApply);
            applied = applied.add(toApply);
            result.add(ContributionDto.from(saved));
        }

        if (applied.signum() > 0) {
            final BigDecimal totalApplied = applied;
            creditPrincipal(tontineId, totalApplied, CashMovementKind.CONTRIBUTION_IN,
                    "Cotisation anticipee membre " + req.memberId(),
                    req.memberId().toString(),
                    fullName(collector));

            memberRepository.findById(req.memberId()).ifPresent(m -> {
                m.setTotalContributed(m.getTotalContributed().add(totalApplied));
                memberRepository.save(m);
            });
        }

        auditService.record(userId, "CONTRIBUTION_ADVANCE", "Member", req.memberId().toString(),
                tontineId, "{\"sessions\":" + req.sessionIds().size() + "}");
        return result;
    }

    private void ensureSessionInTontine(UUID sessionId, UUID tontineId) {
        Session s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));
        if (!s.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Seance hors de la tontine active", HttpStatus.FORBIDDEN);
        }
    }

    private void creditPrincipal(UUID tontineId, BigDecimal amount, CashMovementKind kind,
                                 String description, String reference, String recorderFullName) {
        CashBox principal = cashBoxRepository.findByTontineIdAndType(tontineId, CashBoxType.MAIN)
                .orElseThrow(() -> new ApiException("CASHBOX_NOT_CONFIGURED",
                        "Caisse principale introuvable pour la tontine", HttpStatus.valueOf(422)));
        cashBoxService.credit(principal.getId(), amount, kind, reference, description,
                recorderFullName, tontineId);
    }

    private String fullName(Member m) {
        if (m == null) return "";
        String f = m.getFirstName() == null ? "" : m.getFirstName();
        String l = m.getLastName() == null ? "" : m.getLastName();
        return (f + " " + l).trim();
    }

    public Contribution createPending(UUID tontineId, UUID sessionId, UUID memberId, BigDecimal expectedAmount) {
        Contribution c = new Contribution();
        c.setTontineId(tontineId);
        c.setSessionId(sessionId);
        c.setMemberId(memberId);
        c.setExpectedAmount(expectedAmount == null ? BigDecimal.ZERO : expectedAmount);
        return contributionRepository.save(c);
    }
}
