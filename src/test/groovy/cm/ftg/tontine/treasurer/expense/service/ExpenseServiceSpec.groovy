package cm.ftg.tontine.treasurer.expense.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.auth.entity.UserEntity
import cm.ftg.tontine.auth.repository.UserRepository
import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.tontine.entity.Tontine
import cm.ftg.tontine.tontine.entity.TontineRulesEmbeddable
import cm.ftg.tontine.tontine.repository.TontineRepository
import cm.ftg.tontine.treasurer.cashbox.entity.CashBox
import cm.ftg.tontine.treasurer.cashbox.enums.CashMovementKind
import cm.ftg.tontine.treasurer.cashbox.repository.CashBoxRepository
import cm.ftg.tontine.treasurer.cashbox.service.CashBoxService
import cm.ftg.tontine.treasurer.expense.dto.CreateExpenseRequest
import cm.ftg.tontine.treasurer.expense.entity.Expense
import cm.ftg.tontine.treasurer.expense.enums.ExpenseCategory
import cm.ftg.tontine.treasurer.expense.enums.ExpenseStatus
import cm.ftg.tontine.treasurer.expense.repository.ExpenseRepository
import cm.ftg.tontine.treasurer.security.TreasurerAccessChecker
import spock.lang.Specification
import spock.lang.Subject

class ExpenseServiceSpec extends Specification {

    ExpenseRepository repository = Mock()
    TontineRepository tontineRepository = Mock()
    CashBoxRepository cashBoxRepository = Mock()
    CashBoxService cashBoxService = Mock()
    TreasurerAccessChecker accessChecker = Mock()
    AuditService auditService = Mock()
    UserRepository userRepository = Mock()

    @Subject
    ExpenseService service = new ExpenseService(
            repository, tontineRepository, cashBoxRepository, cashBoxService,
            accessChecker, auditService, userRepository)

    UUID tontineId = UUID.randomUUID()
    UUID userId = UUID.randomUUID()
    UUID cashBoxId = UUID.randomUUID()

    Tontine tontineWithCap(BigDecimal cap) {
        new Tontine().tap {
            rules = new TontineRulesEmbeddable(
                    0.00, 0.00, 0.00,
                    0.00, 0.00, 12,
                    cap,
                    0.00, 0.00)
        }
    }

    def setup() {
        userRepository.findById(userId) >> Optional.of(new UserEntity().tap {
            firstName = 'Tresorier'; lastName = 'X'; email = 't@t.com'
        })
    }

    def "create : en dessous du cap passe direct en PAID et debite la caisse"() {
        given:
        tontineRepository.findById(tontineId) >> Optional.of(tontineWithCap(50000.00))
        cashBoxRepository.findById(cashBoxId) >> Optional.of(new CashBox().tap {
            id = cashBoxId; it.tontineId = this.tontineId
        })
        repository.save(_) >> { Expense e -> e.id = e.id ?: UUID.randomUUID(); e }

        when:
        def dto = service.create(tontineId, userId,
                new CreateExpenseRequest(ExpenseCategory.SUPPLIES, 30000.00,
                        'Achat ramettes', 'Auchan', null, cashBoxId))

        then:
        !dto.needsValidation
        dto.status == ExpenseStatus.PAID
        dto.paidAt != null
        1 * cashBoxService.debit(cashBoxId, 30000.00, CashMovementKind.EXPENSE_OUT,
                _, _, 'Tresorier X', tontineId)
        1 * auditService.record(userId, 'EXPENSE_CREATE', 'Expense', _, tontineId,
                { it.contains('"needsValidation":false') })
    }

    def "create : au-dessus du cap reste en PENDING_VALIDATION et ne debite pas"() {
        given:
        tontineRepository.findById(tontineId) >> Optional.of(tontineWithCap(50000.00))
        cashBoxRepository.findById(cashBoxId) >> Optional.of(new CashBox().tap {
            id = cashBoxId; it.tontineId = this.tontineId
        })
        repository.save(_) >> { Expense e -> e.id = e.id ?: UUID.randomUUID(); e }

        when:
        def dto = service.create(tontineId, userId,
                new CreateExpenseRequest(ExpenseCategory.EVENT, 75000.00,
                        'Buffet', 'Traiteur', null, cashBoxId))

        then:
        dto.needsValidation
        dto.status == ExpenseStatus.PENDING_VALIDATION
        dto.paidAt == null
        0 * cashBoxService.debit(_, _, _, _, _, _, _)
        1 * auditService.record(userId, 'EXPENSE_CREATE', _, _, _,
                { it.contains('"needsValidation":true') })
    }

    def "create : refuse une caisse d'une autre tontine"() {
        given:
        tontineRepository.findById(tontineId) >> Optional.of(tontineWithCap(50000.00))
        cashBoxRepository.findById(cashBoxId) >> Optional.of(new CashBox().tap {
            id = cashBoxId; it.tontineId = UUID.randomUUID()
        })

        when:
        service.create(tontineId, userId,
                new CreateExpenseRequest(ExpenseCategory.OTHER, 1000.00,
                        'x', null, null, cashBoxId))

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
        0 * cashBoxService.debit(_, _, _, _, _, _, _)
    }

    def "create : refuse si les regles de la tontine sont manquantes"() {
        given:
        tontineRepository.findById(tontineId) >> Optional.of(new Tontine())

        when:
        service.create(tontineId, userId,
                new CreateExpenseRequest(ExpenseCategory.OTHER, 1000.00,
                        'x', null, null, cashBoxId))

        then:
        ApiException ex = thrown()
        ex.code == 'TONTINE_RULES_MISSING'
    }
}
