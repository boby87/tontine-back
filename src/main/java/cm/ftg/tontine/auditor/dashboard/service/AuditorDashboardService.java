package cm.ftg.tontine.auditor.dashboard.service;

import cm.ftg.tontine.auditor.anomaly.entity.Anomaly;
import cm.ftg.tontine.auditor.anomaly.enums.AnomalySeverity;
import cm.ftg.tontine.auditor.anomaly.enums.AnomalyStatus;
import cm.ftg.tontine.auditor.anomaly.repository.AnomalyRepository;
import cm.ftg.tontine.auditor.control.entity.Control;
import cm.ftg.tontine.auditor.control.enums.ControlStatus;
import cm.ftg.tontine.auditor.control.repository.ControlRepository;
import cm.ftg.tontine.auditor.dashboard.dto.AuditorDashboardDto;
import cm.ftg.tontine.auditor.dashboard.dto.AuditorDashboardDto.AnomaliesBlock;
import cm.ftg.tontine.auditor.dashboard.dto.AuditorDashboardDto.ControlsBlock;
import cm.ftg.tontine.auditor.dashboard.dto.AuditorDashboardDto.LoansBlock;
import cm.ftg.tontine.auditor.dashboard.dto.AuditorDashboardDto.RecommendationsBlock;
import cm.ftg.tontine.auditor.dashboard.dto.AuditorDashboardDto.ValidationsBlock;
import cm.ftg.tontine.auditor.recommendation.entity.Recommendation;
import cm.ftg.tontine.auditor.recommendation.enums.RecommendationStatus;
import cm.ftg.tontine.auditor.recommendation.repository.RecommendationRepository;
import cm.ftg.tontine.auditor.security.AuditorAccessChecker;
import cm.ftg.tontine.president.validation.enums.ValidationCategory;
import cm.ftg.tontine.president.validation.enums.ValidationStatus;
import cm.ftg.tontine.president.validation.repository.ValidationItemRepository;
import cm.ftg.tontine.treasurer.cashbox.dto.CashBoxDto;
import cm.ftg.tontine.treasurer.cashbox.dto.CashMovementDto;
import cm.ftg.tontine.treasurer.cashbox.entity.CashBox;
import cm.ftg.tontine.treasurer.cashbox.repository.CashBoxRepository;
import cm.ftg.tontine.treasurer.cashbox.repository.CashMovementRepository;
import cm.ftg.tontine.treasurer.loan.entity.Loan;
import cm.ftg.tontine.treasurer.loan.enums.LoanStatus;
import cm.ftg.tontine.treasurer.loan.repository.LoanRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditorDashboardService {

    private static final Set<AnomalyStatus> OPEN_ANOMALY_STATES =
            Set.of(AnomalyStatus.OPEN, AnomalyStatus.IN_RESPONSE);
    private static final Set<RecommendationStatus> ACTIVE_RECOMMENDATION_STATES =
            Set.of(RecommendationStatus.PENDING, RecommendationStatus.IN_PROGRESS, RecommendationStatus.OVERDUE);
    private static final Set<LoanStatus> ACTIVE_LOAN_STATES =
            Set.of(LoanStatus.DISBURSED, LoanStatus.REPAYING);

    private final AuditorAccessChecker accessChecker;
    private final CashBoxRepository cashBoxRepository;
    private final CashMovementRepository cashMovementRepository;
    private final ValidationItemRepository validationRepository;
    private final AnomalyRepository anomalyRepository;
    private final ControlRepository controlRepository;
    private final RecommendationRepository recommendationRepository;
    private final LoanRepository loanRepository;

    public AuditorDashboardService(AuditorAccessChecker accessChecker,
                                   CashBoxRepository cashBoxRepository,
                                   CashMovementRepository cashMovementRepository,
                                   ValidationItemRepository validationRepository,
                                   AnomalyRepository anomalyRepository,
                                   ControlRepository controlRepository,
                                   RecommendationRepository recommendationRepository,
                                   LoanRepository loanRepository) {
        this.accessChecker = accessChecker;
        this.cashBoxRepository = cashBoxRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.validationRepository = validationRepository;
        this.anomalyRepository = anomalyRepository;
        this.controlRepository = controlRepository;
        this.recommendationRepository = recommendationRepository;
        this.loanRepository = loanRepository;
    }

    @Transactional(readOnly = true)
    public AuditorDashboardDto getDashboard(UUID tontineId, UUID userId) {
        accessChecker.requireAuditor(userId, tontineId);

        List<CashBox> boxes = cashBoxRepository.findByTontineIdOrderByTypeAscNameAsc(tontineId);
        BigDecimal totalBalance = boxes.stream()
                .map(CashBox::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<CashBoxDto> boxDtos = boxes.stream().map(CashBoxDto::from).toList();

        long financialPending = validationRepository
                .findByTontineIdAndCategoryAndStatusOrderBySubmittedAtDesc(
                        tontineId, ValidationCategory.FINANCIAL_OPERATION, ValidationStatus.PENDING)
                .size();
        long opinionPending = validationRepository
                .findByTontineIdAndStatusAndAuditorOpinionStatusIsNullOrderBySubmittedAtDesc(
                        tontineId, ValidationStatus.PENDING)
                .size();
        ValidationsBlock validationsBlock = new ValidationsBlock(financialPending, opinionPending);

        List<Anomaly> anomalies = anomalyRepository.findByTontineIdOrderByReportedAtDesc(tontineId);
        long anomaliesOpen = anomalies.stream()
                .filter(a -> OPEN_ANOMALY_STATES.contains(a.getStatus()))
                .count();
        long anomaliesHigh = anomalies.stream()
                .filter(a -> a.getSeverity() == AnomalySeverity.HIGH
                        && a.getStatus() != AnomalyStatus.CLOSED)
                .count();
        AnomaliesBlock anomaliesBlock = new AnomaliesBlock(anomaliesOpen, anomaliesHigh);

        List<Control> controls = controlRepository.findByTontineIdOrderByCreatedAtDesc(tontineId);
        long controlsPlanned = controls.stream()
                .filter(c -> c.getStatus() == ControlStatus.PLANNED)
                .count();
        LocalDate nextDue = controls.stream()
                .filter(c -> c.getStatus() == ControlStatus.PLANNED && c.getPeriodTo() != null)
                .map(Control::getPeriodTo)
                .min(Comparator.naturalOrder())
                .orElse(null);
        ControlsBlock controlsBlock = new ControlsBlock(controlsPlanned, nextDue);

        List<Recommendation> recommendations = recommendationRepository
                .findByTontineIdOrderByCreatedAtDesc(tontineId);
        long recoActive = recommendations.stream()
                .filter(r -> ACTIVE_RECOMMENDATION_STATES.contains(r.getStatus()))
                .count();
        long recoTotal = recommendations.size();
        long recoImplemented = recommendations.stream()
                .filter(r -> r.getStatus() == RecommendationStatus.IMPLEMENTED
                        || r.getStatus() == RecommendationStatus.CLOSED)
                .count();
        BigDecimal implementedRate = (recoTotal == 0) ? BigDecimal.ZERO
                : BigDecimal.valueOf(recoImplemented * 100L)
                        .divide(BigDecimal.valueOf(recoTotal), 2, RoundingMode.HALF_UP);
        RecommendationsBlock recommendationsBlock = new RecommendationsBlock(recoActive, implementedRate);

        List<Loan> loans = loanRepository.findByTontineIdOrderByRequestedAtDesc(tontineId);
        long loansActive = loans.stream()
                .filter(l -> ACTIVE_LOAN_STATES.contains(l.getStatus()))
                .count();
        Instant now = Instant.now();
        long loansOverdue = loans.stream()
                .filter(l -> ACTIVE_LOAN_STATES.contains(l.getStatus()))
                .filter(l -> l.getDueDate() != null && l.getDueDate().isBefore(now))
                .count();
        BigDecimal totalOutstanding = loans.stream()
                .filter(l -> ACTIVE_LOAN_STATES.contains(l.getStatus()))
                .map(l -> l.getTotalDue().subtract(l.getTotalRepaid() != null
                        ? l.getTotalRepaid() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LoansBlock loansBlock = new LoansBlock(loansActive, loansOverdue, totalOutstanding);

        List<CashMovementDto> recentMovements = cashMovementRepository
                .findTop10ByTontineIdOrderByRecordedAtDesc(tontineId).stream()
                .map(CashMovementDto::from)
                .toList();

        return new AuditorDashboardDto(boxDtos, totalBalance, validationsBlock,
                anomaliesBlock, controlsBlock, recommendationsBlock, loansBlock, recentMovements);
    }
}
