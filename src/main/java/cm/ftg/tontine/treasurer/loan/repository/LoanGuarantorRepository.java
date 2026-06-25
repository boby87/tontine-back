package cm.ftg.tontine.treasurer.loan.repository;

import cm.ftg.tontine.treasurer.loan.entity.LoanGuarantor;
import cm.ftg.tontine.treasurer.loan.enums.GuarantorStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanGuarantorRepository extends JpaRepository<LoanGuarantor, UUID> {

    List<LoanGuarantor> findByLoanId(UUID loanId);

    Optional<LoanGuarantor> findByLoanIdAndGuarantorId(UUID loanId, UUID guarantorId);

    boolean existsByLoanIdAndGuarantorIdAndStatus(UUID loanId, UUID guarantorId, GuarantorStatus status);

    long countByLoanIdAndStatus(UUID loanId, GuarantorStatus status);

    long countByLoanId(UUID loanId);

    void deleteByLoanId(UUID loanId);
}
