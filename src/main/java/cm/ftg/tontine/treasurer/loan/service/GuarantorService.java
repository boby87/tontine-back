package cm.ftg.tontine.treasurer.loan.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.treasurer.loan.dto.LoanGuarantorDto;
import cm.ftg.tontine.treasurer.loan.entity.Loan;
import cm.ftg.tontine.treasurer.loan.entity.LoanGuarantor;
import cm.ftg.tontine.treasurer.loan.enums.GuarantorStatus;
import cm.ftg.tontine.treasurer.loan.enums.LoanStatus;
import cm.ftg.tontine.treasurer.loan.repository.LoanGuarantorRepository;
import cm.ftg.tontine.treasurer.loan.repository.LoanRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuarantorService {

    private final LoanGuarantorRepository guarantorRepository;
    private final LoanRepository loanRepository;
    private final AuditService auditService;

    public GuarantorService(LoanGuarantorRepository guarantorRepository,
                            LoanRepository loanRepository,
                            AuditService auditService) {
        this.guarantorRepository = guarantorRepository;
        this.loanRepository = loanRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void addGuarantors(UUID loanId, List<UUID> guarantorMemberIds) {
        if (guarantorMemberIds == null || guarantorMemberIds.isEmpty()) {
            return;
        }
        for (UUID memberId : guarantorMemberIds) {
            LoanGuarantor g = new LoanGuarantor();
            g.setLoanId(loanId);
            g.setGuarantorId(memberId);
            g.setStatus(GuarantorStatus.PENDING);
            guarantorRepository.save(g);
        }
    }

    @Transactional(readOnly = true)
    public List<LoanGuarantorDto> listByLoan(UUID loanId, UUID tontineId) {
        Loan loan = loadInTontine(loanId, tontineId);
        return guarantorRepository.findByLoanId(loan.getId()).stream()
                .map(LoanGuarantorDto::from)
                .toList();
    }

    @Transactional
    public LoanGuarantorDto respond(UUID loanId, UUID tontineId, UUID guarantorMemberId,
                                    boolean accepted, String rejectionReason) {
        Loan loan = loadInTontine(loanId, tontineId);
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new ApiException("LOAN_NOT_PENDING",
                    "Ce pret n'attend plus de reponse de garant", HttpStatus.CONFLICT);
        }

        LoanGuarantor guarantor = guarantorRepository
                .findByLoanIdAndGuarantorId(loanId, guarantorMemberId)
                .orElseThrow(() -> new ApiException("GUARANTOR_NOT_FOUND",
                        "Vous n'etes pas garant de ce pret", HttpStatus.FORBIDDEN));

        if (guarantor.getStatus() != GuarantorStatus.PENDING) {
            throw new ApiException("GUARANTOR_ALREADY_RESPONDED",
                    "Vous avez deja repondu a cette demande de cautionnement", HttpStatus.CONFLICT);
        }

        guarantor.setStatus(accepted ? GuarantorStatus.ACCEPTED : GuarantorStatus.REJECTED);
        guarantor.setRespondedAt(Instant.now());
        if (!accepted && rejectionReason != null) {
            guarantor.setRejectionReason(rejectionReason);
        }
        LoanGuarantor saved = guarantorRepository.save(guarantor);

        try {
            auditService.record(guarantorMemberId, accepted ? "GUARANTOR_ACCEPT" : "GUARANTOR_REJECT",
                    "LoanGuarantor", saved.getId().toString(), tontineId,
                    "{\"loanId\":\"" + loanId + "\"}");
        } catch (Exception ignored) {
        }

        return LoanGuarantorDto.from(saved);
    }

    public boolean areAllAccepted(UUID loanId) {
        long total = guarantorRepository.countByLoanId(loanId);
        if (total == 0) return true;
        long accepted = guarantorRepository.countByLoanIdAndStatus(loanId, GuarantorStatus.ACCEPTED);
        return accepted == total;
    }

    public boolean hasAnyRejected(UUID loanId) {
        return guarantorRepository.countByLoanIdAndStatus(loanId, GuarantorStatus.REJECTED) > 0;
    }

    private Loan loadInTontine(UUID loanId, UUID tontineId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", loanId));
        if (!loan.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Pret hors de la tontine active", HttpStatus.FORBIDDEN);
        }
        return loan;
    }
}
