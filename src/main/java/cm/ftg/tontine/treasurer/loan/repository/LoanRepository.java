package cm.ftg.tontine.treasurer.loan.repository;

import cm.ftg.tontine.treasurer.loan.entity.Loan;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    List<Loan> findByTontineIdOrderByRequestedAtDesc(UUID tontineId);

    long countByTontineIdAndStatusIn(UUID tontineId,
            java.util.List<cm.ftg.tontine.treasurer.loan.enums.LoanStatus> statuses);
}
