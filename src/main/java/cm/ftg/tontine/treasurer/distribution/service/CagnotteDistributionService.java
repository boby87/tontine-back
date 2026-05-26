package cm.ftg.tontine.treasurer.distribution.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auth.entity.UserEntity;
import cm.ftg.tontine.auth.repository.UserRepository;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
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
import cm.ftg.tontine.treasurer.distribution.dto.CagnotteDistributionDto;
import cm.ftg.tontine.treasurer.distribution.dto.CreateDistributionRequest;
import cm.ftg.tontine.treasurer.distribution.entity.CagnotteDistribution;
import cm.ftg.tontine.treasurer.distribution.repository.CagnotteDistributionRepository;
import cm.ftg.tontine.treasurer.security.TreasurerAccessChecker;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CagnotteDistributionService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final CagnotteDistributionRepository repository;
    private final SessionRepository sessionRepository;
    private final TontineRepository tontineRepository;
    private final MemberRepository memberRepository;
    private final CashBoxRepository cashBoxRepository;
    private final CashBoxService cashBoxService;
    private final TreasurerAccessChecker accessChecker;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public CagnotteDistributionService(CagnotteDistributionRepository repository,
                                       SessionRepository sessionRepository,
                                       TontineRepository tontineRepository,
                                       MemberRepository memberRepository,
                                       CashBoxRepository cashBoxRepository,
                                       CashBoxService cashBoxService,
                                       TreasurerAccessChecker accessChecker,
                                       AuditService auditService,
                                       UserRepository userRepository) {
        this.repository = repository;
        this.sessionRepository = sessionRepository;
        this.tontineRepository = tontineRepository;
        this.memberRepository = memberRepository;
        this.cashBoxRepository = cashBoxRepository;
        this.cashBoxService = cashBoxService;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CagnotteDistributionDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireTreasurer(userId, tontineId);
        return repository.findByTontineIdOrderByCreatedAtDesc(tontineId).stream()
                .map(CagnotteDistributionDto::from)
                .toList();
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

        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new ResourceNotFoundException("Tontine", tontineId));
        if (tontine.getRules() == null) {
            throw new ApiException("TONTINE_RULES_MISSING",
                    "Regles de la tontine indisponibles", HttpStatus.CONFLICT);
        }

        BigDecimal gross = session.getTotalCollected() == null ? BigDecimal.ZERO : session.getTotalCollected();
        BigDecimal emergencyPct = nullSafe(tontine.getRules().getEmergencyDeductionPercent());
        BigDecimal operationsPct = nullSafe(tontine.getRules().getOperationsDeductionPercent());

        BigDecimal deductionEmergency = gross.multiply(emergencyPct).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal deductionOperations = gross.multiply(operationsPct).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal netAmount = gross.subtract(deductionEmergency).subtract(deductionOperations);
        if (netAmount.signum() < 0) {
            netAmount = BigDecimal.ZERO;
        }

        Member beneficiary = memberRepository.findById(session.getBeneficiaryMemberId())
                .orElseThrow(() -> new ApiException("NO_BENEFICIARY",
                        "Beneficiaire introuvable", HttpStatus.CONFLICT));
        String beneficiaryFullName = (beneficiary.getFirstName() + " " + beneficiary.getLastName()).trim();
        if (beneficiaryFullName.isEmpty()) {
            beneficiaryFullName = beneficiary.getMatricule();
        }

        CashBox principal = cashBoxRepository.findByTontineIdAndType(tontineId, CashBoxType.PRINCIPAL)
                .orElseThrow(() -> new ApiException("NO_PRINCIPAL_CASHBOX",
                        "Caisse principale absente pour cette tontine", HttpStatus.CONFLICT));

        UserEntity actor = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND",
                        "Utilisateur introuvable", HttpStatus.NOT_FOUND));
        String actorFullName = buildFullName(actor);

        Instant now = Instant.now();

        CagnotteDistribution d = new CagnotteDistribution();
        d.setSessionId(session.getId());
        d.setSessionNumber(session.getNumber());
        d.setTontineId(tontineId);
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

        auditService.record(userId, "CAGNOTTE_DISTRIBUTE", "CagnotteDistribution",
                saved.getId().toString(), tontineId,
                "{\"sessionId\":\"" + session.getId() + "\",\"net\":\"" + netAmount + "\"}");

        return CagnotteDistributionDto.from(saved);
    }

    private BigDecimal nullSafe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String buildFullName(UserEntity u) {
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last = u.getLastName() == null ? "" : u.getLastName();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? u.getEmail() : full;
    }
}
