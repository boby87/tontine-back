package cm.ftg.tontine.treasurer.loan.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.member.entity.Member
import cm.ftg.tontine.treasurer.cashbox.entity.CashBox
import cm.ftg.tontine.treasurer.cashbox.enums.CashBoxType
import cm.ftg.tontine.treasurer.cashbox.enums.CashMovementKind
import cm.ftg.tontine.treasurer.cashbox.repository.CashBoxRepository
import cm.ftg.tontine.treasurer.cashbox.service.CashBoxService
import cm.ftg.tontine.treasurer.common.enums.PaymentMethod
import cm.ftg.tontine.treasurer.loan.dto.DisburseLoanRequest
import cm.ftg.tontine.treasurer.loan.dto.RepayLoanRequest
import cm.ftg.tontine.treasurer.loan.entity.Loan
import cm.ftg.tontine.treasurer.loan.enums.LoanStatus
import cm.ftg.tontine.treasurer.loan.repository.LoanRepository
import cm.ftg.tontine.treasurer.security.TreasurerAccessChecker
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class LoanServiceSpec extends Specification {

    LoanRepository loanRepository = Mock()
    CashBoxRepository cashBoxRepository = Mock()
    CashBoxService cashBoxService = Mock()
    TreasurerAccessChecker accessChecker = Mock()
    AuditService auditService = Mock()

    @Subject
    LoanService service = new LoanService(
            loanRepository, cashBoxRepository, cashBoxService, accessChecker, auditService)

    UUID tontineId = UUID.randomUUID()
    UUID userId = UUID.randomUUID()
    UUID loanId = UUID.randomUUID()
    UUID principalCashBoxId = UUID.randomUUID()

    Member treasurer = new Member().tap { firstName = 'Ange'; lastName = 'Mbida' }

    def setup() {
        accessChecker.requireTreasurer(userId, tontineId) >> treasurer
        cashBoxRepository.findByTontineIdAndType(tontineId, CashBoxType.PRINCIPAL) >>
                Optional.of(new CashBox().tap { id = principalCashBoxId; type = CashBoxType.PRINCIPAL })
    }

    Loan loan(LoanStatus status, BigDecimal principal = 100000.00,
              BigDecimal totalDue = 110000.00, BigDecimal totalRepaid = 0.00) {
        new Loan().tap {
            id = loanId
            it.tontineId = this.tontineId
            it.memberId = UUID.randomUUID()
            it.principal = principal
            it.totalDue = totalDue
            it.totalRepaid = totalRepaid
            it.status = status
        }
    }

    @Unroll
    def "disburse : refuse un pret dans l'etat #status"() {
        given:
        def l = loan(status)
        loanRepository.findById(loanId) >> Optional.of(l)

        when:
        service.disburse(loanId, tontineId, userId, new DisburseLoanRequest(PaymentMethod.CASH))

        then:
        ApiException ex = thrown()
        ex.code == 'LOAN_INVALID_STATE'
        ex.status.value() == 409
        0 * cashBoxService.debit(_, _, _, _, _, _, _)

        where:
        status << [LoanStatus.REQUESTED, LoanStatus.REJECTED, LoanStatus.DISBURSED,
                   LoanStatus.REPAYING, LoanStatus.REPAID]
    }

    def "disburse : passe en DISBURSED et debite la caisse principale"() {
        given:
        def l = loan(LoanStatus.APPROVED, 100000.00)
        loanRepository.findById(loanId) >> Optional.of(l)
        loanRepository.save(_) >> { it[0] }

        when:
        def dto = service.disburse(loanId, tontineId, userId,
                new DisburseLoanRequest(PaymentMethod.MOBILE_MONEY))

        then:
        dto.status == LoanStatus.DISBURSED
        l.disbursedAt != null
        1 * cashBoxService.debit(principalCashBoxId, 100000.00, CashMovementKind.LOAN_DISBURSEMENT_OUT,
                _, _, 'Ange Mbida', tontineId)
        1 * auditService.record(userId, 'LOAN_DISBURSE', 'Loan', loanId.toString(),
                tontineId, _)
    }

    def "repay : passe en REPAYING tant que le total n'est pas atteint"() {
        given:
        def l = loan(LoanStatus.DISBURSED, 100000.00, 110000.00, 30000.00)
        loanRepository.findById(loanId) >> Optional.of(l)
        loanRepository.save(_) >> { it[0] }

        when:
        def dto = service.repay(loanId, tontineId, userId,
                new RepayLoanRequest(40000.00, PaymentMethod.CASH))

        then:
        dto.status == LoanStatus.REPAYING
        l.totalRepaid == 70000.00
        1 * cashBoxService.credit(principalCashBoxId, 40000.00, CashMovementKind.LOAN_REPAYMENT_IN,
                _, _, 'Ange Mbida', tontineId)
    }

    def "repay : passe en REPAID quand le total est atteint"() {
        given:
        def l = loan(LoanStatus.REPAYING, 100000.00, 110000.00, 80000.00)
        loanRepository.findById(loanId) >> Optional.of(l)
        loanRepository.save(_) >> { it[0] }

        when:
        def dto = service.repay(loanId, tontineId, userId,
                new RepayLoanRequest(30000.00, PaymentMethod.CASH))

        then:
        dto.status == LoanStatus.REPAID
        l.totalRepaid == 110000.00
    }

    def "repay : refuse un pret qui n'est pas DISBURSED ou REPAYING"() {
        given:
        def l = loan(LoanStatus.APPROVED)
        loanRepository.findById(loanId) >> Optional.of(l)

        when:
        service.repay(loanId, tontineId, userId,
                new RepayLoanRequest(1000.00, PaymentMethod.CASH))

        then:
        ApiException ex = thrown()
        ex.code == 'LOAN_INVALID_STATE'
    }

    def "createPending : calcule le total et la mensualite"() {
        given:
        loanRepository.save(_) >> { Loan l -> l.id = UUID.randomUUID(); l }

        when:
        def saved = service.createPending(tontineId, UUID.randomUUID(),
                120000.00, 10.00, 12, 'Investissement', null)

        then:
        // interest = 120000 * (10/100) * 12 / 12 = 12000
        saved.totalDue == 132000.00
        saved.monthlyPayment == 11000.00      // 132000 / 12
        saved.status == LoanStatus.REQUESTED
    }

    def "createPending : duree zero rend totalDue == principal et monthlyPayment == totalDue"() {
        given:
        loanRepository.save(_) >> { Loan l -> l.id = UUID.randomUUID(); l }

        when:
        def saved = service.createPending(tontineId, UUID.randomUUID(),
                50000.00, 10.00, 0, 'flash', null)

        then:
        saved.totalDue == 50000.00
        saved.monthlyPayment == 50000.00
    }

    def "disburse : refuse un pret d'une autre tontine"() {
        given:
        def l = loan(LoanStatus.APPROVED).tap { it.tontineId = UUID.randomUUID() }
        loanRepository.findById(loanId) >> Optional.of(l)

        when:
        service.disburse(loanId, tontineId, userId, new DisburseLoanRequest(PaymentMethod.CASH))

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
    }
}
