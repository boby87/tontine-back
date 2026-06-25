package cm.ftg.tontine.treasurer.loan.repository;

import cm.ftg.tontine.treasurer.loan.entity.LoanRepaymentSchedule;
import cm.ftg.tontine.treasurer.loan.enums.LoanRepaymentScheduleStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanRepaymentScheduleRepository extends JpaRepository<LoanRepaymentSchedule, UUID> {

    List<LoanRepaymentSchedule> findByLoanIdOrderByInstallmentNumber(UUID loanId);

    List<LoanRepaymentSchedule> findByLoanIdAndStatusInOrderByInstallmentNumber(
            UUID loanId, List<LoanRepaymentScheduleStatus> statuses);

    boolean existsByLoanId(UUID loanId);
}
