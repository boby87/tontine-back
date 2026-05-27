package cm.ftg.tontine.treasurer.cashbox.service

import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.realtime.RealtimeEventPublisher
import cm.ftg.tontine.treasurer.cashbox.entity.CashBox
import cm.ftg.tontine.treasurer.cashbox.entity.CashMovement
import cm.ftg.tontine.treasurer.cashbox.enums.CashMovementKind
import cm.ftg.tontine.treasurer.cashbox.enums.MovementDirection
import cm.ftg.tontine.treasurer.cashbox.repository.CashBoxRepository
import cm.ftg.tontine.treasurer.cashbox.repository.CashMovementRepository
import cm.ftg.tontine.treasurer.security.TreasurerAccessChecker
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class CashBoxServiceSpec extends Specification {

    CashBoxRepository cashBoxRepository = Mock()
    CashMovementRepository movementRepository = Mock()
    TreasurerAccessChecker accessChecker = Mock()
    RealtimeEventPublisher realtime = Mock()

    @Subject
    CashBoxService service = new CashBoxService(
            cashBoxRepository, movementRepository, accessChecker, realtime)

    UUID tontineId = UUID.randomUUID()
    UUID cashBoxId = UUID.randomUUID()

    CashBox box(BigDecimal balance, boolean locked = false) {
        new CashBox().tap {
            it.id = cashBoxId
            it.tontineId = this.tontineId
            it.name = 'Principale'
            it.balance = balance
            it.isLocked = locked
        }
    }

    def "credit : augmente le solde et persiste un mouvement IN"() {
        given:
        def b = box(100.00)
        cashBoxRepository.findById(cashBoxId) >> Optional.of(b)
        movementRepository.save(_) >> { CashMovement m -> m }

        when:
        def m = service.credit(cashBoxId, 50.00 as BigDecimal, CashMovementKind.CONTRIBUTION_IN,
                'ref-1', 'cotisation', 'Tresorier', tontineId)

        then:
        b.balance == 150.00
        1 * cashBoxRepository.save(b)
        m.direction == MovementDirection.IN
        m.amount == 50.00
        m.balanceAfter == 150.00
        1 * realtime.toTreasurerDashboard(tontineId, 'cashbox.updated', _ as Map)
    }

    def "debit : diminue le solde et persiste un mouvement OUT"() {
        given:
        def b = box(200.00)
        cashBoxRepository.findById(cashBoxId) >> Optional.of(b)
        movementRepository.save(_) >> { CashMovement m -> m }

        when:
        def m = service.debit(cashBoxId, 80.00 as BigDecimal, CashMovementKind.CAGNOTTE_OUT,
                'ref-2', 'distribution', 'Tresorier', tontineId)

        then:
        b.balance == 120.00
        m.direction == MovementDirection.OUT
        m.balanceAfter == 120.00
    }

    def "debit : refuse quand le solde est insuffisant"() {
        given:
        def b = box(20.00)
        cashBoxRepository.findById(cashBoxId) >> Optional.of(b)

        when:
        service.debit(cashBoxId, 50.00 as BigDecimal, CashMovementKind.CAGNOTTE_OUT,
                'ref-3', 'fail', 'Tresorier', tontineId)

        then:
        ApiException ex = thrown()
        ex.code == 'INSUFFICIENT_BALANCE'
        ex.status.value() == 422
        0 * cashBoxRepository.save(_)
        0 * movementRepository.save(_)
    }

    def "debit : refuse quand la caisse est verrouillee"() {
        given:
        def b = box(1000.00, true)
        cashBoxRepository.findById(cashBoxId) >> Optional.of(b)

        when:
        service.debit(cashBoxId, 10.00 as BigDecimal, CashMovementKind.CAGNOTTE_OUT,
                'ref-4', 'locked', 'Tresorier', tontineId)

        then:
        ApiException ex = thrown()
        ex.code == 'CASHBOX_LOCKED'
    }

    @Unroll
    def "debit/credit : rejette un montant non strictement positif (#amount)"() {
        given:
        def b = box(1000.00)
        cashBoxRepository.findById(cashBoxId) >> Optional.of(b)

        when:
        service.credit(cashBoxId, amount as BigDecimal, CashMovementKind.CONTRIBUTION_IN,
                'ref', 'desc', 'Tresorier', tontineId)

        then:
        ApiException ex = thrown()
        ex.code == 'INVALID_AMOUNT'
        ex.status.value() == 422

        where:
        amount << [0.00, -10.00, -0.01]
    }

    def "credit : refuse quand la caisse appartient a une autre tontine"() {
        given:
        def b = box(0.00).tap { it.tontineId = UUID.randomUUID() }
        cashBoxRepository.findById(cashBoxId) >> Optional.of(b)

        when:
        service.credit(cashBoxId, 10.00 as BigDecimal, CashMovementKind.CONTRIBUTION_IN,
                'ref', 'desc', 'Tresorier', tontineId)

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
    }
}
