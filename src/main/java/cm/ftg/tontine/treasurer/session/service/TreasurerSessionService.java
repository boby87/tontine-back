package cm.ftg.tontine.treasurer.session.service;

import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.president.sanction.enums.SanctionStatus;
import cm.ftg.tontine.president.sanction.repository.SanctionRepository;
import cm.ftg.tontine.president.session.entity.Session;
import cm.ftg.tontine.president.session.repository.SessionRepository;
import cm.ftg.tontine.treasurer.cashbox.repository.CashBoxRepository;
import cm.ftg.tontine.treasurer.contribution.enums.ContributionStatus;
import cm.ftg.tontine.treasurer.contribution.repository.ContributionRepository;
import cm.ftg.tontine.treasurer.security.TreasurerAccessChecker;
import cm.ftg.tontine.treasurer.session.dto.CashBoxBalanceDto;
import cm.ftg.tontine.treasurer.session.dto.SessionFinancialReportDto;
import cm.ftg.tontine.treasurer.session.dto.SessionSummaryDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TreasurerSessionService {

    private final SessionRepository sessionRepository;
    private final ContributionRepository contributionRepository;
    private final SanctionRepository sanctionRepository;
    private final CashBoxRepository cashBoxRepository;
    private final TreasurerAccessChecker accessChecker;

    public TreasurerSessionService(SessionRepository sessionRepository,
                                    ContributionRepository contributionRepository,
                                    SanctionRepository sanctionRepository,
                                    CashBoxRepository cashBoxRepository,
                                    TreasurerAccessChecker accessChecker) {
        this.sessionRepository = sessionRepository;
        this.contributionRepository = contributionRepository;
        this.sanctionRepository = sanctionRepository;
        this.cashBoxRepository = cashBoxRepository;
        this.accessChecker = accessChecker;
    }

    @Transactional(readOnly = true)
    public List<SessionSummaryDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireTreasurer(userId, tontineId);
        return sessionRepository.findByTontineIdOrderByNumberAsc(tontineId).stream()
                .map(SessionSummaryDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SessionFinancialReportDto bilan(UUID sessionId, UUID tontineId, UUID userId) {
        accessChecker.requireTreasurer(userId, tontineId);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));
        if (!session.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Session hors de la tontine active",
                    HttpStatus.FORBIDDEN);
        }

        BigDecimal totalContributions = nullToZero(contributionRepository
                .sumPaidAmountBySessionAndStatuses(sessionId,
                        List.of(ContributionStatus.PAID, ContributionStatus.PARTIAL)));
        BigDecimal totalExtraContributions = BigDecimal.ZERO;
        BigDecimal totalSanctions = nullToZero(sanctionRepository
                .sumAmountBySessionAndStatuses(sessionId, List.of(SanctionStatus.PAID)));
        BigDecimal totalRepayments = BigDecimal.ZERO;
        BigDecimal totalIncome = totalContributions
                .add(totalExtraContributions)
                .add(totalSanctions)
                .add(totalRepayments);

        BigDecimal totalDistribution = nullToZero(session.getTotalDistributed());
        BigDecimal totalExpenses = BigDecimal.ZERO;
        BigDecimal totalDisbursements = BigDecimal.ZERO;
        BigDecimal totalOutflows = totalDistribution
                .add(totalExpenses)
                .add(totalDisbursements);

        BigDecimal netResult = totalIncome.subtract(totalOutflows);

        List<CashBoxBalanceDto> cashBoxBalances = cashBoxRepository
                .findByTontineIdOrderByTypeAscNameAsc(tontineId).stream()
                .map(b -> new CashBoxBalanceDto(b.getName(), b.getBalance()))
                .toList();

        return new SessionFinancialReportDto(
                session.getId(),
                session.getNumber(),
                session.getScheduledAt(),
                totalContributions,
                totalExtraContributions,
                totalSanctions,
                totalRepayments,
                totalIncome,
                totalDistribution,
                totalExpenses,
                totalDisbursements,
                totalOutflows,
                netResult,
                cashBoxBalances,
                false,
                false,
                null,
                null);
    }

    private BigDecimal nullToZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
