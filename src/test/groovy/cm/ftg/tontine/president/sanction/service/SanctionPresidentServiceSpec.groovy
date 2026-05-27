package cm.ftg.tontine.president.sanction.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.member.entity.Member
import cm.ftg.tontine.member.repository.MemberRepository
import cm.ftg.tontine.notification.enums.NotificationCategory
import cm.ftg.tontine.notification.enums.NotificationKind
import cm.ftg.tontine.notification.service.NotificationService
import cm.ftg.tontine.president.sanction.dto.WaiveSanctionRequest
import cm.ftg.tontine.president.sanction.entity.Sanction
import cm.ftg.tontine.president.sanction.enums.SanctionCancelByRole
import cm.ftg.tontine.president.sanction.enums.SanctionStatus
import cm.ftg.tontine.president.sanction.enums.SanctionType
import cm.ftg.tontine.president.sanction.repository.SanctionRepository
import cm.ftg.tontine.president.security.PresidentAccessChecker
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class SanctionPresidentServiceSpec extends Specification {

    SanctionRepository repository = Mock()
    PresidentAccessChecker accessChecker = Mock()
    AuditService auditService = Mock()
    MemberRepository memberRepository = Mock()
    NotificationService notificationService = Mock()

    @Subject
    SanctionPresidentService service = new SanctionPresidentService(
            repository, accessChecker, auditService, memberRepository, notificationService)

    UUID tontineId = UUID.randomUUID()
    UUID userId = UUID.randomUUID()
    UUID sanctionId = UUID.randomUUID()
    UUID memberId = UUID.randomUUID()
    UUID memberUserId = UUID.randomUUID()

    Sanction sanction(SanctionStatus status, boolean financial = true) {
        new Sanction().tap {
            id = sanctionId
            it.tontineId = this.tontineId
            it.memberId = this.memberId
            it.financial = financial
            type = SanctionType.ABSENCE
            amount = 5000.00
            reason = 'Absence non justifiee'
            it.status = status
        }
    }

    private void stubMemberWithUser() {
        memberRepository.findById(memberId) >> Optional.of(new Member().tap {
            id = memberId; userId = memberUserId
        })
    }

    def "waive : passe en WAIVED, initie le remboursement, notifie le membre"() {
        given:
        def s = sanction(SanctionStatus.CONFIRMED, true)
        repository.findById(sanctionId) >> Optional.of(s)
        repository.save(_) >> { it[0] }
        stubMemberWithUser()

        when:
        def dto = service.waive(sanctionId, tontineId, userId, 'Pierre Pdt',
                new WaiveSanctionRequest('Erreur de saisie'))

        then:
        dto.status == SanctionStatus.WAIVED
        s.cancelledAt != null
        s.cancelledByUserId == userId
        s.cancelledByFullName == 'Pierre Pdt'
        s.cancelReason == 'Erreur de saisie'
        s.cancelledByRole == SanctionCancelByRole.PRESIDENT
        s.refundInitiated     // financial=true => refund initie
        1 * auditService.record(userId, 'SANCTION_WAIVE', 'Sanction', sanctionId.toString(),
                tontineId, { it.contains('Erreur de saisie') })
        1 * notificationService.publish(memberUserId, tontineId, NotificationKind.SUCCESS,
                NotificationCategory.SANCTION, 'Sanction levee',
                { it.contains('ABSENCE') && it.contains('Erreur de saisie') },
                _ as String)
    }

    def "waive : sanction non financiere n'initie pas de remboursement"() {
        given:
        def s = sanction(SanctionStatus.PENDING, false)
        repository.findById(sanctionId) >> Optional.of(s)
        repository.save(_) >> { it[0] }
        stubMemberWithUser()

        when:
        service.waive(sanctionId, tontineId, userId, 'Pdt',
                new WaiveSanctionRequest('rs'))

        then:
        !s.refundInitiated
    }

    @Unroll
    def "waive : refuse les etats finaux (#status)"() {
        given:
        def s = sanction(status)
        repository.findById(sanctionId) >> Optional.of(s)

        when:
        service.waive(sanctionId, tontineId, userId, 'P',
                new WaiveSanctionRequest('motif'))

        then:
        ApiException ex = thrown()
        ex.code == 'SANCTION_FINAL_STATE'
        ex.status.value() == 409
        0 * repository.save(_)

        where:
        status << [SanctionStatus.WAIVED, SanctionStatus.CANCELLED, SanctionStatus.PAID]
    }

    @Unroll
    def "confirm : autorise depuis #status"() {
        given:
        def s = sanction(status)
        repository.findById(sanctionId) >> Optional.of(s)
        repository.save(_) >> { it[0] }
        stubMemberWithUser()

        when:
        def dto = service.confirm(sanctionId, tontineId, userId)

        then:
        dto.status == SanctionStatus.CONFIRMED
        s.resolvedByUserId == userId
        1 * notificationService.publish(memberUserId, tontineId, NotificationKind.WARNING,
                NotificationCategory.SANCTION, 'Sanction confirmee', _ as String, _ as String)
        1 * auditService.record(userId, 'SANCTION_CONFIRM', 'Sanction', sanctionId.toString(),
                tontineId, null)

        where:
        status << [SanctionStatus.PENDING, SanctionStatus.CONTESTED]
    }

    def "confirm : refuse depuis un autre etat"() {
        given:
        def s = sanction(SanctionStatus.CONFIRMED)
        repository.findById(sanctionId) >> Optional.of(s)

        when:
        service.confirm(sanctionId, tontineId, userId)

        then:
        ApiException ex = thrown()
        ex.code == 'SANCTION_INVALID_STATE'
    }

    def "waive : refuse une sanction d'une autre tontine"() {
        given:
        def s = sanction(SanctionStatus.PENDING).tap { it.tontineId = UUID.randomUUID() }
        repository.findById(sanctionId) >> Optional.of(s)

        when:
        service.waive(sanctionId, tontineId, userId, 'P',
                new WaiveSanctionRequest('x'))

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
    }

    def "waive : ne notifie pas quand le membre n'a pas d'utilisateur lie"() {
        given:
        def s = sanction(SanctionStatus.PENDING)
        repository.findById(sanctionId) >> Optional.of(s)
        repository.save(_) >> { it[0] }
        memberRepository.findById(memberId) >> Optional.of(new Member().tap { id = memberId; userId = null })

        when:
        service.waive(sanctionId, tontineId, userId, 'P',
                new WaiveSanctionRequest('motif'))

        then:
        0 * notificationService.publish(_, _, _, _, _, _, _)
    }
}
