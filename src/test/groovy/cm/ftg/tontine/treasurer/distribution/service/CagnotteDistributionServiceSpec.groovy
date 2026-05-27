package cm.ftg.tontine.treasurer.distribution.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.auth.entity.UserEntity
import cm.ftg.tontine.auth.repository.UserRepository
import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.member.entity.Member
import cm.ftg.tontine.member.repository.MemberRepository
import cm.ftg.tontine.president.session.entity.Session
import cm.ftg.tontine.president.session.enums.SessionStatus
import cm.ftg.tontine.president.session.repository.SessionRepository
import cm.ftg.tontine.tontine.entity.Tontine
import cm.ftg.tontine.tontine.entity.TontineRulesEmbeddable
import cm.ftg.tontine.tontine.repository.TontineRepository
import cm.ftg.tontine.treasurer.cashbox.entity.CashBox
import cm.ftg.tontine.treasurer.cashbox.enums.CashBoxType
import cm.ftg.tontine.treasurer.cashbox.enums.CashMovementKind
import cm.ftg.tontine.treasurer.cashbox.repository.CashBoxRepository
import cm.ftg.tontine.treasurer.cashbox.service.CashBoxService
import cm.ftg.tontine.treasurer.common.enums.PaymentMethod
import cm.ftg.tontine.treasurer.distribution.dto.CreateDistributionRequest
import cm.ftg.tontine.treasurer.distribution.entity.CagnotteDistribution
import cm.ftg.tontine.treasurer.distribution.repository.CagnotteDistributionRepository
import cm.ftg.tontine.treasurer.security.TreasurerAccessChecker
import spock.lang.Specification
import spock.lang.Subject

class CagnotteDistributionServiceSpec extends Specification {

    CagnotteDistributionRepository repository = Mock()
    SessionRepository sessionRepository = Mock()
    TontineRepository tontineRepository = Mock()
    MemberRepository memberRepository = Mock()
    CashBoxRepository cashBoxRepository = Mock()
    CashBoxService cashBoxService = Mock()
    TreasurerAccessChecker accessChecker = Mock()
    AuditService auditService = Mock()
    UserRepository userRepository = Mock()

    @Subject
    CagnotteDistributionService service = new CagnotteDistributionService(
            repository, sessionRepository, tontineRepository, memberRepository,
            cashBoxRepository, cashBoxService, accessChecker, auditService, userRepository)

    UUID tontineId = UUID.randomUUID()
    UUID userId = UUID.randomUUID()
    UUID sessionId = UUID.randomUUID()
    UUID beneficiaryId = UUID.randomUUID()
    UUID cashBoxId = UUID.randomUUID()

    Session signedSession(BigDecimal collected = 100000.00, SessionStatus status = SessionStatus.IN_PROGRESS) {
        new Session().tap {
            id = sessionId
            it.tontineId = this.tontineId
            number = 4
            cagnotteSignedByPresident = true
            beneficiaryMemberId = beneficiaryId
            totalCollected = collected
            it.status = status
            totalDistributed = 0.00
        }
    }

    Tontine tontineWithRules(BigDecimal emergencyPct, BigDecimal opsPct) {
        new Tontine().tap {
            rules = new TontineRulesEmbeddable(
                    0.00, 0.00, 0.00,
                    0.00, 0.00, 12,
                    50000.00,
                    emergencyPct, opsPct)
        }
    }

    Member beneficiary() {
        new Member().tap {
            id = beneficiaryId
            firstName = 'Alice'
            lastName = 'Tchoumi'
            phone = '+237699111222'
        }
    }

    UserEntity user() {
        new UserEntity().tap {
            firstName = 'Tresorier'
            lastName = 'TestUser'
            email = 't@t.com'
        }
    }

    def "create : refuse si la cagnotte n'est pas signee"() {
        given:
        def s = signedSession().tap { cagnotteSignedByPresident = false }
        sessionRepository.findById(sessionId) >> Optional.of(s)

        when:
        service.create(tontineId, userId,
                new CreateDistributionRequest(sessionId, PaymentMethod.CASH, '123456'))

        then:
        ApiException ex = thrown()
        ex.code == 'CAGNOTTE_NOT_SIGNED'
    }

    def "create : refuse si aucun beneficiaire"() {
        given:
        def s = signedSession().tap { beneficiaryMemberId = null }
        sessionRepository.findById(sessionId) >> Optional.of(s)

        when:
        service.create(tontineId, userId,
                new CreateDistributionRequest(sessionId, PaymentMethod.CASH, '123456'))

        then:
        ApiException ex = thrown()
        ex.code == 'NO_BENEFICIARY'
    }

    def "create : calcule net = brut - emergency - operations et debite la caisse"() {
        given:
        def s = signedSession(100000.00)
        sessionRepository.findById(sessionId) >> Optional.of(s)
        tontineRepository.findById(tontineId) >> Optional.of(tontineWithRules(10.00, 5.00))
        memberRepository.findById(beneficiaryId) >> Optional.of(beneficiary())
        cashBoxRepository.findByTontineIdAndType(tontineId, CashBoxType.PRINCIPAL) >>
                Optional.of(new CashBox().tap { id = cashBoxId; type = CashBoxType.PRINCIPAL })
        userRepository.findById(userId) >> Optional.of(user())
        repository.save(_) >> { CagnotteDistribution d -> d.id = UUID.randomUUID(); d }

        when:
        def dto = service.create(tontineId, userId,
                new CreateDistributionRequest(sessionId, PaymentMethod.MOBILE_MONEY, '123456'))

        then:
        dto.grossAmount == 100000.00
        dto.deductionEmergency == 10000.00     // 10%
        dto.deductionOperations == 5000.00     // 5%
        dto.netAmount == 85000.00
        dto.beneficiaryFullName == 'Alice Tchoumi'
        1 * cashBoxService.debit(cashBoxId, 85000.00, CashMovementKind.CAGNOTTE_OUT,
                _, _, 'Tresorier TestUser', tontineId)
        s.totalDistributed == 85000.00
        s.status == SessionStatus.COMPLETED
        s.endedAt != null
    }

    def "create : ne debite pas la caisse si net <= 0"() {
        given:
        def s = signedSession(0.00)
        sessionRepository.findById(sessionId) >> Optional.of(s)
        tontineRepository.findById(tontineId) >> Optional.of(tontineWithRules(0.00, 0.00))
        memberRepository.findById(beneficiaryId) >> Optional.of(beneficiary())
        cashBoxRepository.findByTontineIdAndType(tontineId, CashBoxType.PRINCIPAL) >>
                Optional.of(new CashBox().tap { id = cashBoxId; type = CashBoxType.PRINCIPAL })
        userRepository.findById(userId) >> Optional.of(user())
        repository.save(_) >> { CagnotteDistribution d -> d.id = UUID.randomUUID(); d }

        when:
        def dto = service.create(tontineId, userId,
                new CreateDistributionRequest(sessionId, PaymentMethod.CASH, '123456'))

        then:
        dto.netAmount == 0.00
        0 * cashBoxService.debit(_, _, _, _, _, _, _)
    }

    def "create : ne change pas le status si la session n'est pas IN_PROGRESS"() {
        given:
        def s = signedSession(50000.00, SessionStatus.SCHEDULED)
        sessionRepository.findById(sessionId) >> Optional.of(s)
        tontineRepository.findById(tontineId) >> Optional.of(tontineWithRules(0.00, 0.00))
        memberRepository.findById(beneficiaryId) >> Optional.of(beneficiary())
        cashBoxRepository.findByTontineIdAndType(tontineId, CashBoxType.PRINCIPAL) >>
                Optional.of(new CashBox().tap { id = cashBoxId; type = CashBoxType.PRINCIPAL })
        userRepository.findById(userId) >> Optional.of(user())
        repository.save(_) >> { CagnotteDistribution d -> d.id = UUID.randomUUID(); d }

        when:
        service.create(tontineId, userId,
                new CreateDistributionRequest(sessionId, PaymentMethod.CASH, '123456'))

        then:
        s.status == SessionStatus.SCHEDULED
        s.endedAt == null
    }

    def "create : refuse si caisse principale absente"() {
        given:
        def s = signedSession()
        sessionRepository.findById(sessionId) >> Optional.of(s)
        tontineRepository.findById(tontineId) >> Optional.of(tontineWithRules(0.00, 0.00))
        memberRepository.findById(beneficiaryId) >> Optional.of(beneficiary())
        cashBoxRepository.findByTontineIdAndType(tontineId, CashBoxType.PRINCIPAL) >> Optional.empty()

        when:
        service.create(tontineId, userId,
                new CreateDistributionRequest(sessionId, PaymentMethod.CASH, '123456'))

        then:
        ApiException ex = thrown()
        ex.code == 'NO_PRINCIPAL_CASHBOX'
    }
}
