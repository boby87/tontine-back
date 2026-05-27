package cm.ftg.tontine.treasurer.contribution.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.member.entity.Member
import cm.ftg.tontine.member.repository.MemberRepository
import cm.ftg.tontine.president.session.entity.Session
import cm.ftg.tontine.president.session.repository.SessionRepository
import cm.ftg.tontine.tontine.entity.Tontine
import cm.ftg.tontine.tontine.repository.TontineRepository
import cm.ftg.tontine.treasurer.cashbox.entity.CashBox
import cm.ftg.tontine.treasurer.cashbox.enums.CashBoxType
import cm.ftg.tontine.treasurer.cashbox.enums.CashMovementKind
import cm.ftg.tontine.treasurer.cashbox.repository.CashBoxRepository
import cm.ftg.tontine.treasurer.cashbox.service.CashBoxService
import cm.ftg.tontine.treasurer.common.enums.PaymentMethod
import cm.ftg.tontine.treasurer.contribution.dto.AdvancePaymentRequest
import cm.ftg.tontine.treasurer.contribution.dto.PayContributionRequest
import cm.ftg.tontine.treasurer.contribution.entity.Contribution
import cm.ftg.tontine.treasurer.contribution.enums.ContributionStatus
import cm.ftg.tontine.treasurer.contribution.repository.ContributionRepository
import cm.ftg.tontine.treasurer.security.TreasurerAccessChecker
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class ContributionServiceSpec extends Specification {

    ContributionRepository contributionRepository = Mock()
    SessionRepository sessionRepository = Mock()
    MemberRepository memberRepository = Mock()
    TontineRepository tontineRepository = Mock()
    CashBoxRepository cashBoxRepository = Mock()
    CashBoxService cashBoxService = Mock()
    TreasurerAccessChecker accessChecker = Mock()
    AuditService auditService = Mock()

    @Subject
    ContributionService service = new ContributionService(
            contributionRepository, sessionRepository, memberRepository, tontineRepository,
            cashBoxRepository, cashBoxService, accessChecker, auditService)

    UUID tontineId = UUID.randomUUID()
    UUID userId = UUID.randomUUID()
    UUID memberId = UUID.randomUUID()
    UUID sessionId = UUID.randomUUID()
    UUID contributionId = UUID.randomUUID()
    UUID principalCashBoxId = UUID.randomUUID()

    Member treasurer = new Member().tap { firstName = 'Marc'; lastName = 'Eyenga' }

    def setup() {
        accessChecker.requireTreasurer(userId, tontineId) >> treasurer
        cashBoxRepository.findByTontineIdAndType(tontineId, CashBoxType.PRINCIPAL) >>
                Optional.of(new CashBox().tap { id = principalCashBoxId; type = CashBoxType.PRINCIPAL })
    }

    Contribution contribution(BigDecimal expected, BigDecimal paid = 0.00,
                              ContributionStatus status = ContributionStatus.PENDING) {
        new Contribution().tap {
            id = contributionId
            it.tontineId = this.tontineId
            it.memberId = this.memberId
            it.sessionId = this.sessionId
            expectedAmount = expected
            paidAmount = paid
            it.status = status
        }
    }

    Session session(BigDecimal collected = 0.00) {
        new Session().tap {
            id = sessionId
            it.tontineId = this.tontineId
            totalCollected = collected
        }
    }

    def "pay : paiement partiel laisse le status PARTIAL"() {
        given:
        def c = contribution(1000.00)
        contributionRepository.findById(contributionId) >> Optional.of(c)
        sessionRepository.findById(sessionId) >> Optional.of(session())
        memberRepository.findById(memberId) >> Optional.of(new Member().tap { totalContributed = 0.00 })
        contributionRepository.save(_) >> { it[0] }

        when:
        def dto = service.pay(contributionId, tontineId, userId,
                new PayContributionRequest(300.00, PaymentMethod.CASH, null, null))

        then:
        dto.status == ContributionStatus.PARTIAL
        dto.paidAmount == 300.00
        c.paidAt == null
        1 * cashBoxService.credit(principalCashBoxId, 300.00, CashMovementKind.CONTRIBUTION_IN,
                _, _, 'Marc Eyenga', tontineId)
    }

    def "pay : paiement couvrant le total passe en PAID + paidAt + maj membre + maj session"() {
        given:
        def c = contribution(1000.00, 700.00, ContributionStatus.PARTIAL)
        def s = session(500.00)
        def m = new Member().tap { totalContributed = 700.00 }
        contributionRepository.findById(contributionId) >> Optional.of(c)
        sessionRepository.findById(sessionId) >> Optional.of(s)
        memberRepository.findById(memberId) >> Optional.of(m)
        contributionRepository.save(_) >> { it[0] }

        when:
        def dto = service.pay(contributionId, tontineId, userId,
                new PayContributionRequest(400.00, PaymentMethod.MOBILE_MONEY, 'ref-X', 'note'))

        then:
        dto.status == ContributionStatus.PAID
        c.paidAt != null
        c.reference == 'ref-X'
        c.note == 'note'
        s.totalCollected == 900.00       // 500 + 400
        m.totalContributed == 1100.00    // 700 + 400
        1 * sessionRepository.save(s)
        1 * memberRepository.save(m)
        1 * auditService.record(userId, 'CONTRIBUTION_PAY', 'Contribution', contributionId.toString(),
                tontineId, _)
    }

    @Unroll
    def "pay : refuse une cotisation deja reglee (#status)"() {
        given:
        def c = contribution(1000.00, 1000.00, status)
        contributionRepository.findById(contributionId) >> Optional.of(c)

        when:
        service.pay(contributionId, tontineId, userId,
                new PayContributionRequest(100.00, PaymentMethod.CASH, null, null))

        then:
        ApiException ex = thrown()
        ex.code == 'CONTRIBUTION_ALREADY_SETTLED'
        ex.status.value() == 409
        0 * cashBoxService.credit(_, _, _, _, _, _, _)

        where:
        status << [ContributionStatus.PAID, ContributionStatus.EXEMPTED]
    }

    def "pay : refuse une cotisation d'une autre tontine"() {
        given:
        def c = contribution(1000.00).tap { it.tontineId = UUID.randomUUID() }
        contributionRepository.findById(contributionId) >> Optional.of(c)

        when:
        service.pay(contributionId, tontineId, userId,
                new PayContributionRequest(100.00, PaymentMethod.CASH, null, null))

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
    }

    def "advance : repartit un montant sur plusieurs sessions"() {
        given:
        def tontine = new Tontine().tap { contributionAmount = 500.00 }
        def s1 = new Session().tap { id = UUID.randomUUID(); it.tontineId = this.tontineId; totalCollected = 0.00 }
        def s2 = new Session().tap { id = UUID.randomUUID(); it.tontineId = this.tontineId; totalCollected = 0.00 }
        def s3 = new Session().tap { id = UUID.randomUUID(); it.tontineId = this.tontineId; totalCollected = 0.00 }
        tontineRepository.findById(tontineId) >> Optional.of(tontine)
        sessionRepository.findById(s1.id) >> Optional.of(s1)
        sessionRepository.findById(s2.id) >> Optional.of(s2)
        sessionRepository.findById(s3.id) >> Optional.of(s3)
        contributionRepository.findBySessionIdAndMemberId(_, memberId) >> Optional.empty()
        contributionRepository.save(_) >> { Contribution c -> c.id = c.id ?: UUID.randomUUID(); c }
        memberRepository.findById(memberId) >> Optional.of(new Member().tap { totalContributed = 0.00 })

        when:
        def list = service.advance(tontineId, userId,
                new AdvancePaymentRequest(memberId, [s1.id, s2.id, s3.id], 1200.00, PaymentMethod.CASH))

        then:
        list.size() == 3
        list[0].status == ContributionStatus.PAID      // 500/500
        list[1].status == ContributionStatus.PAID      // 500/500
        list[2].status == ContributionStatus.PARTIAL   // 200/500
        s1.totalCollected == 500.00
        s2.totalCollected == 500.00
        s3.totalCollected == 200.00
        // credit caisse principale unique pour 1200
        1 * cashBoxService.credit(principalCashBoxId, 1200.00, CashMovementKind.CONTRIBUTION_IN,
                _, _, 'Marc Eyenga', tontineId)
        1 * auditService.record(userId, 'CONTRIBUTION_ADVANCE', 'Member', memberId.toString(),
                tontineId, { it.contains('"sessions":3') })
    }

    def "advance : ne credite pas la caisse quand le montant applique est nul"() {
        given:
        def tontine = new Tontine().tap { contributionAmount = 500.00 }
        def s = new Session().tap { id = UUID.randomUUID(); it.tontineId = this.tontineId; totalCollected = 0.00 }
        def existing = new Contribution().tap {
            id = UUID.randomUUID()
            it.tontineId = this.tontineId
            it.memberId = this.memberId
            it.sessionId = s.id
            expectedAmount = 500.00
            paidAmount = 500.00
            status = ContributionStatus.PAID
        }
        tontineRepository.findById(tontineId) >> Optional.of(tontine)
        sessionRepository.findById(s.id) >> Optional.of(s)
        contributionRepository.findBySessionIdAndMemberId(s.id, memberId) >> Optional.of(existing)
        contributionRepository.save(_) >> { it[0] }

        when:
        def list = service.advance(tontineId, userId,
                new AdvancePaymentRequest(memberId, [s.id], 500.00, PaymentMethod.CASH))

        then:
        list.size() == 1
        list[0].status == ContributionStatus.PAID
        0 * cashBoxService.credit(_, _, _, _, _, _, _)
    }
}
