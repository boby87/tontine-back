package cm.ftg.tontine.treasurer.mobilemoney.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.integration.mobilemoney.gateway.MobileMoneyGateway
import cm.ftg.tontine.integration.mobilemoney.gateway.PaymentResult
import cm.ftg.tontine.integration.mobilemoney.gateway.PaymentStatus
import cm.ftg.tontine.member.entity.Member
import cm.ftg.tontine.realtime.RealtimeEventPublisher
import cm.ftg.tontine.treasurer.cashbox.entity.CashBox
import cm.ftg.tontine.treasurer.cashbox.enums.CashBoxType
import cm.ftg.tontine.treasurer.cashbox.repository.CashBoxRepository
import cm.ftg.tontine.treasurer.cashbox.service.CashBoxService
import cm.ftg.tontine.treasurer.mobilemoney.dto.ApproveMobileMoneyRequest
import cm.ftg.tontine.treasurer.mobilemoney.dto.MobileMoneySendRequest
import cm.ftg.tontine.treasurer.mobilemoney.entity.MobileMoneyTransaction
import cm.ftg.tontine.treasurer.mobilemoney.enums.MobileMoneyProvider
import cm.ftg.tontine.treasurer.mobilemoney.enums.MobileMoneyStatus
import cm.ftg.tontine.treasurer.mobilemoney.repository.MobileMoneyTransactionRepository
import cm.ftg.tontine.treasurer.security.TreasurerAccessChecker
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class MobileMoneyServiceSpec extends Specification {

    MobileMoneyTransactionRepository repository = Mock()
    CashBoxService cashBoxService = Mock()
    CashBoxRepository cashBoxRepository = Mock()
    TreasurerAccessChecker accessChecker = Mock()
    AuditService auditService = Mock()
    RealtimeEventPublisher realtime = Mock()
    MobileMoneyGateway gateway = Mock()

    @Subject
    MobileMoneyService service = new MobileMoneyService(
            repository, cashBoxService, cashBoxRepository, accessChecker,
            auditService, realtime, gateway)

    UUID tontineId = UUID.randomUUID()
    UUID userId = UUID.randomUUID()
    Member treasurer = new Member().tap { firstName = 'Jean'; lastName = 'Dupont' }

    def setup() {
        accessChecker.requireTreasurer(userId, tontineId) >> treasurer
    }

    def "send : echoue quand le PIN est vide"() {
        given:
        def req = new MobileMoneySendRequest(MobileMoneyProvider.MTN_MOMO, '+237699000001', 1000.00, 'paiement', '')

        when:
        service.send(tontineId, userId, req)

        then:
        ApiException ex = thrown()
        ex.code == 'MOBILE_MONEY_INVALID_PIN'
        0 * gateway.disburse(_, _, _, _, _)
    }

    @Unroll
    def "send : mappe PaymentStatus #status vers MobileMoneyStatus #expected"() {
        given:
        def req = new MobileMoneySendRequest(MobileMoneyProvider.MTN_MOMO, '+237699000001', 1000.00, 'paiement', '1234')
        gateway.disburse(MobileMoneyProvider.MTN_MOMO, '+237699000001', 1000.00, _, _) >>
                new PaymentResult('gw-ref', status, 'msg')
        repository.save(_) >> { MobileMoneyTransaction t -> t.id = UUID.randomUUID(); t }

        when:
        def dto = service.send(tontineId, userId, req)

        then:
        dto.status == expected
        dto.externalReference == 'gw-ref'
        dto.reviewedByFullName == 'Jean Dupont'

        where:
        status                | expected
        PaymentStatus.SUCCESS | MobileMoneyStatus.COMPLETED
        PaymentStatus.PENDING | MobileMoneyStatus.APPROVED
        PaymentStatus.UNKNOWN | MobileMoneyStatus.APPROVED
    }

    def "send : leve une 422 quand la gateway retourne FAILED"() {
        given:
        def req = new MobileMoneySendRequest(MobileMoneyProvider.MTN_MOMO, '+237699000001', 1000.00, 'paiement', '1234')
        gateway.disburse(_, _, _, _, _) >> new PaymentResult('gw-ref', PaymentStatus.FAILED, 'KO')

        when:
        service.send(tontineId, userId, req)

        then:
        ApiException ex = thrown()
        ex.code == 'MOBILE_MONEY_GATEWAY_FAILED'
        ex.status.value() == 422
        0 * repository.save(_)
    }

    def "approve : refuse une transaction qui n'est pas PENDING_APPROVAL"() {
        given:
        def tx = new MobileMoneyTransaction().tap {
            id = UUID.randomUUID()
            it.tontineId = this.tontineId
            status = MobileMoneyStatus.COMPLETED
        }
        repository.findById(tx.id) >> Optional.of(tx)

        when:
        service.approve(tx.id, tontineId, userId, new ApproveMobileMoneyRequest(null))

        then:
        ApiException ex = thrown()
        ex.code == 'MOBILE_MONEY_INVALID_STATE'
    }

    def "approve : credite la caisse principale et passe la transaction en COMPLETED"() {
        given:
        def tx = new MobileMoneyTransaction().tap {
            id = UUID.randomUUID()
            it.tontineId = this.tontineId
            status = MobileMoneyStatus.PENDING_APPROVAL
            amount = 5000.00
            provider = MobileMoneyProvider.MTN_MOMO
            externalReference = 'EXT-1'
        }
        def principal = new CashBox().tap { id = UUID.randomUUID(); type = CashBoxType.PRINCIPAL }
        repository.findById(tx.id) >> Optional.of(tx)
        cashBoxRepository.findByTontineIdAndType(tontineId, CashBoxType.PRINCIPAL) >> Optional.of(principal)
        repository.save(_) >> { MobileMoneyTransaction t -> if (!t.id) t.id = UUID.randomUUID(); t }

        when:
        def dto = service.approve(tx.id, tontineId, userId, new ApproveMobileMoneyRequest(null))

        then:
        1 * cashBoxService.credit(principal.id, 5000.00, _, 'EXT-1', _, 'Jean Dupont', tontineId)
        dto.status == MobileMoneyStatus.COMPLETED
        1 * realtime.toTreasurerDashboard(tontineId, 'mobile_money.received', _)
        1 * auditService.record(userId, 'MOBILE_MONEY_APPROVE', 'MobileMoneyTransaction', _, tontineId, _)
    }
}
