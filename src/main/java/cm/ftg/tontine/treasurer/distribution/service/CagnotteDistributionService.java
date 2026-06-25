package cm.ftg.tontine.treasurer.distribution.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auth.entity.UserEntity;
import cm.ftg.tontine.auth.repository.UserRepository;
import cm.ftg.tontine.auth.service.OtpService;
import cm.ftg.tontine.common.enums.AuctionSacrificeDestination;
import cm.ftg.tontine.common.enums.DistributionMode;
import cm.ftg.tontine.common.enums.OtpPurpose;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.common.enums.MemberStatus;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.president.session.entity.Session;
import cm.ftg.tontine.president.session.enums.SessionStatus;
import cm.ftg.tontine.president.session.repository.SessionRepository;
import cm.ftg.tontine.tontine.entity.Tontine;
import cm.ftg.tontine.tontine.repository.TontineRepository;
import cm.ftg.tontine.treasurer.cashbox.entity.CashBox;
import cm.ftg.tontine.treasurer.cashbox.enums.CashBoxType;
import cm.ftg.tontine.treasurer.cashbox.enums.CashMovementKind;
import cm.ftg.tontine.treasurer.cashbox.repository.CashBoxRepository;
import cm.ftg.tontine.treasurer.cashbox.service.CashBoxService;
import cm.ftg.tontine.treasurer.distribution.dto.AuctionBidRequest;
import cm.ftg.tontine.treasurer.distribution.dto.BeneficiarySelectionDto;
import cm.ftg.tontine.treasurer.distribution.dto.CagnotteDistributionDto;
import cm.ftg.tontine.treasurer.distribution.dto.CreateDistributionRequest;
import cm.ftg.tontine.treasurer.distribution.dto.RunAuctionRequest;
import cm.ftg.tontine.treasurer.distribution.dto.RunLotteryRequest;
import cm.ftg.tontine.treasurer.distribution.entity.CagnotteDistribution;
import cm.ftg.tontine.treasurer.distribution.repository.CagnotteDistributionRepository;
import cm.ftg.tontine.treasurer.security.TreasurerAccessChecker;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CagnotteDistributionService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CagnotteDistributionRepository repository;
    private final SessionRepository sessionRepository;
    private final TontineRepository tontineRepository;
    private final MemberRepository memberRepository;
    private final CashBoxRepository cashBoxRepository;
    private final CashBoxService cashBoxService;
    private final TreasurerAccessChecker accessChecker;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final OtpService otpService;

    public CagnotteDistributionService(CagnotteDistributionRepository repository,
                                       SessionRepository sessionRepository,
                                       TontineRepository tontineRepository,
                                       MemberRepository memberRepository,
                                       CashBoxRepository cashBoxRepository,
                                       CashBoxService cashBoxService,
                                       TreasurerAccessChecker accessChecker,
                                       AuditService auditService,
                                       UserRepository userRepository,
                                       OtpService otpService) {
        this.repository = repository;
        this.sessionRepository = sessionRepository;
        this.tontineRepository = tontineRepository;
        this.memberRepository = memberRepository;
        this.cashBoxRepository = cashBoxRepository;
        this.cashBoxService = cashBoxService;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.otpService = otpService;
    }

    @Transactional(readOnly = true)
    public List<CagnotteDistributionDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireTreasurer(userId, tontineId);
        return repository.findByTontineIdOrderByCreatedAtDesc(tontineId).stream()
                .map(CagnotteDistributionDto::from)
                .toList();
    }

    /**
     * AUCTION mode — selects the highest bidder as beneficiary, handles sacrifice fund routing,
     * and sends OTP to the winner so they can confirm distribution.
     */
    @Transactional
    public BeneficiarySelectionDto runAuction(UUID tontineId, UUID userId, RunAuctionRequest req) {
        accessChecker.requireTreasurer(userId, tontineId);

        Session session = loadAndValidateSession(tontineId, req.sessionId());
        requireSessionNotAlreadyDistributed(session);

        Tontine tontine = loadTontine(tontineId);
        if (tontine.getDistributionMode() != DistributionMode.AUCTION) {
            throw new ApiException("WRONG_DISTRIBUTION_MODE",
                    "Cette tontine n'est pas configuree en mode encheres", HttpStatus.CONFLICT);
        }

        AuctionBidRequest winningBid = req.bids().stream()
                .max(Comparator.comparing(AuctionBidRequest::sacrificeAmount))
                .orElseThrow(() -> new ApiException("NO_BIDS", "Aucune mise reçue", HttpStatus.valueOf(422)));

        Member winner = memberRepository.findById(winningBid.memberId())
                .filter(m -> m.getTontineId().equals(tontineId))
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new ApiException("INVALID_WINNER",
                        "Le membre gagnant est introuvable ou inactif", HttpStatus.CONFLICT));

        BigDecimal sacrifice = winningBid.sacrificeAmount();
        UserEntity actor = loadUser(userId);
        String actorName = buildFullName(actor);

        CashBox principal = loadPrincipalCashBox(tontineId);
        AuctionSacrificeDestination destination = tontine.getAuctionSacrificeDestination();
        if (destination == null) {
            destination = AuctionSacrificeDestination.TREASURY;
        }
        cashBoxService.credit(principal.getId(), sacrifice, CashMovementKind.AUCTION_SACRIFICE,
                "AUCTION_SACRIFICE:" + session.getId(),
                "Sacrifice enchère session #" + session.getNumber() + " (" + destination.name() + ")",
                actorName, tontineId);

        session.setBeneficiaryMemberId(winner.getId());
        session.setBeneficiaryFullName(buildMemberFullName(winner));
        sessionRepository.save(session);

        String beneficiaryIdentifier = winner.getPhone() != null ? winner.getPhone() : winner.getEmail();
        try {
            otpService.issue(beneficiaryIdentifier, OtpPurpose.CAGNOTTE_DISTRIBUTION);
        } catch (RuntimeException e) {
            // OTP dispatch failure is non-blocking; treasurer can trigger resend
        }

        try {
            auditService.record(userId, "AUCTION_WINNER_SELECTED", "Session",
                    session.getId().toString(), tontineId,
                    "{\"winner\":\"" + winner.getId() + "\",\"sacrifice\":\"" + sacrifice + "\"}");
        } catch (RuntimeException ignored) {
        }

        return new BeneficiarySelectionDto(session.getId(), winner.getId(),
                buildMemberFullName(winner), winner.getPhone(), DistributionMode.AUCTION, sacrifice);
    }

    /**
     * LOTTERY mode — randomly selects from members who have not yet received the cagnotte
     * in the current cycle, then sends OTP to the selected beneficiary.
     */
    @Transactional
    public BeneficiarySelectionDto runLottery(UUID tontineId, UUID userId, RunLotteryRequest req) {
        accessChecker.requireTreasurer(userId, tontineId);

        Session session = loadAndValidateSession(tontineId, req.sessionId());
        requireSessionNotAlreadyDistributed(session);

        Tontine tontine = loadTontine(tontineId);
        if (tontine.getDistributionMode() != DistributionMode.LOTTERY) {
            throw new ApiException("WRONG_DISTRIBUTION_MODE",
                    "Cette tontine n'est pas configuree en mode tirage au sort", HttpStatus.CONFLICT);
        }

        Set<UUID> alreadyReceived = repository.findBeneficiaryMemberIdsByCycleId(session.getCycleId());

        List<Member> eligible = memberRepository.findByTontineId(tontineId).stream()
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .filter(m -> !alreadyReceived.contains(m.getId()))
                .toList();

        if (eligible.isEmpty()) {
            throw new ApiException("NO_ELIGIBLE_MEMBERS",
                    "Tous les membres ont deja reçu leur cagnotte ce cycle", HttpStatus.CONFLICT);
        }

        Member selected = eligible.get(RANDOM.nextInt(eligible.size()));

        session.setBeneficiaryMemberId(selected.getId());
        session.setBeneficiaryFullName(buildMemberFullName(selected));
        sessionRepository.save(session);

        String beneficiaryIdentifier = selected.getPhone() != null ? selected.getPhone() : selected.getEmail();
        try {
            otpService.issue(beneficiaryIdentifier, OtpPurpose.CAGNOTTE_DISTRIBUTION);
        } catch (RuntimeException e) {
            // OTP dispatch failure is non-blocking
        }

        try {
            auditService.record(userId, "LOTTERY_WINNER_SELECTED", "Session",
                    session.getId().toString(), tontineId,
                    "{\"winner\":\"" + selected.getId() + "\",\"eligibleCount\":" + eligible.size() + "}");
        } catch (RuntimeException ignored) {
        }

        return new BeneficiarySelectionDto(session.getId(), selected.getId(),
                buildMemberFullName(selected), selected.getPhone(), DistributionMode.LOTTERY, null);
    }

    @Transactional
    public CagnotteDistributionDto create(UUID tontineId, UUID userId, CreateDistributionRequest req) {
        accessChecker.requireTreasurer(userId, tontineId);

        Session session = sessionRepository.findById(req.sessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session", req.sessionId()));
        if (!session.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Session hors de la tontine active", HttpStatus.FORBIDDEN);
        }
        if (!session.isCagnotteSignedByPresident()) {
            throw new ApiException("CAGNOTTE_NOT_SIGNED",
                    "La cagnotte n'a pas ete signee par le president", HttpStatus.CONFLICT);
        }
        if (session.getBeneficiaryMemberId() == null) {
            throw new ApiException("NO_BENEFICIARY",
                    "Aucun beneficiaire defini pour cette session", HttpStatus.CONFLICT);
        }

        Tontine tontine = loadTontine(tontineId);
        if (tontine.getRules() == null) {
            throw new ApiException("TONTINE_RULES_MISSING",
                    "Regles de la tontine indisponibles", HttpStatus.CONFLICT);
        }

        Member beneficiary = memberRepository.findById(session.getBeneficiaryMemberId())
                .orElseThrow(() -> new ApiException("NO_BENEFICIARY",
                        "Beneficiaire introuvable", HttpStatus.CONFLICT));

        // OTP verification — the beneficiary must confirm via their registered identifier
        String beneficiaryIdentifier = beneficiary.getPhone() != null
                ? beneficiary.getPhone() : beneficiary.getEmail();
        otpService.verifyAndConsume(beneficiaryIdentifier, req.otp(), OtpPurpose.CAGNOTTE_DISTRIBUTION);

        BigDecimal gross = session.getTotalCollected() == null ? BigDecimal.ZERO : session.getTotalCollected();
        BigDecimal emergencyPct = nullSafe(tontine.getRules().getEmergencyDeductionPercent());
        BigDecimal operationsPct = nullSafe(tontine.getRules().getOperationsDeductionPercent());

        BigDecimal deductionEmergency = gross.multiply(emergencyPct).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal deductionOperations = gross.multiply(operationsPct).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal netAmount = gross.subtract(deductionEmergency).subtract(deductionOperations);
        if (netAmount.signum() < 0) {
            netAmount = BigDecimal.ZERO;
        }

        String beneficiaryFullName = buildMemberFullName(beneficiary);

        CashBox principal = loadPrincipalCashBox(tontineId);

        UserEntity actor = loadUser(userId);
        String actorFullName = buildFullName(actor);

        Instant now = Instant.now();

        CagnotteDistribution d = new CagnotteDistribution();
        d.setSessionId(session.getId());
        d.setSessionNumber(session.getNumber());
        d.setCycleId(session.getCycleId());
        d.setTontineId(tontineId);
        d.setDistributionMode(tontine.getDistributionMode());
        d.setBeneficiaryMemberId(beneficiary.getId());
        d.setBeneficiaryFullName(beneficiaryFullName);
        d.setBeneficiaryPhone(beneficiary.getPhone() == null ? "" : beneficiary.getPhone());
        d.setGrossAmount(gross);
        d.setDeductionEmergency(deductionEmergency);
        d.setDeductionOperations(deductionOperations);
        d.setNetAmount(netAmount);
        d.setPaymentMethod(req.paymentMethod());
        d.setBeneficiaryConfirmed(true);
        d.setBeneficiaryConfirmedAt(now);
        d.setTreasurerPaidAt(now);
        CagnotteDistribution saved = repository.save(d);

        if (netAmount.signum() > 0) {
            cashBoxService.debit(principal.getId(), netAmount, CashMovementKind.CAGNOTTE_OUT,
                    "DISTRIBUTION:" + saved.getId(),
                    "Versement cagnotte session #" + session.getNumber() + " a " + beneficiaryFullName,
                    actorFullName, tontineId);
        }

        session.setTotalDistributed(netAmount);
        if (session.getStatus() == SessionStatus.IN_PROGRESS) {
            session.setStatus(SessionStatus.COMPLETED);
            session.setEndedAt(now);
        }
        sessionRepository.save(session);

        try {
            auditService.record(userId, "CAGNOTTE_DISTRIBUTE", "CagnotteDistribution",
                    saved.getId().toString(), tontineId,
                    "{\"sessionId\":\"" + session.getId() + "\",\"net\":\"" + netAmount + "\"}");
        } catch (RuntimeException ignored) {
        }

        return CagnotteDistributionDto.from(saved);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Session loadAndValidateSession(UUID tontineId, UUID sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));
        if (!session.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Session hors de la tontine active", HttpStatus.FORBIDDEN);
        }
        return session;
    }

    private void requireSessionNotAlreadyDistributed(Session session) {
        if (session.getTotalDistributed() != null && session.getTotalDistributed().signum() > 0) {
            throw new ApiException("ALREADY_DISTRIBUTED",
                    "La cagnotte de cette session a deja ete versee", HttpStatus.CONFLICT);
        }
    }

    private Tontine loadTontine(UUID tontineId) {
        return tontineRepository.findById(tontineId)
                .orElseThrow(() -> new ResourceNotFoundException("Tontine", tontineId));
    }

    private CashBox loadPrincipalCashBox(UUID tontineId) {
        return cashBoxRepository.findByTontineIdAndType(tontineId, CashBoxType.MAIN)
                .orElseThrow(() -> new ApiException("NO_PRINCIPAL_CASHBOX",
                        "Caisse principale absente pour cette tontine", HttpStatus.CONFLICT));
    }

    private UserEntity loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND",
                        "Utilisateur introuvable", HttpStatus.NOT_FOUND));
    }

    private BigDecimal nullSafe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String buildMemberFullName(Member m) {
        String first = m.getFirstName() == null ? "" : m.getFirstName();
        String last = m.getLastName() == null ? "" : m.getLastName();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? m.getMatricule() : full;
    }

    private String buildFullName(UserEntity u) {
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last = u.getLastName() == null ? "" : u.getLastName();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? u.getEmail() : full;
    }
}
