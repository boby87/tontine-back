package cm.ftg.tontine.censor.contestation.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.censor.contestation.dto.DecideContestationRequest
import cm.ftg.tontine.censor.contestation.enums.ContestationDecision
import cm.ftg.tontine.censor.security.CensorAccessChecker
import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.president.sanction.entity.Sanction
import cm.ftg.tontine.president.sanction.enums.SanctionCancelByRole
import cm.ftg.tontine.president.sanction.enums.SanctionStatus
import cm.ftg.tontine.president.sanction.enums.SanctionType
import cm.ftg.tontine.president.sanction.repository.SanctionRepository
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class ContestationCensorServiceSpec extends Specification {

    SanctionRepository sanctionRepository = Mock()
    CensorAccessChecker accessChecker = Mock()
    AuditService auditService = Mock()

    @Subject
    ContestationCensorService service = new ContestationCensorService(
            sanctionRepository, accessChecker, auditService)

    UUID tontineId = UUID.randomUUID()
    UUID userId = UUID.randomUUID()
    UUID sanctionId = UUID.randomUUID()

    Sanction contested(boolean financial = true) {
        new Sanction().tap {
            id = sanctionId
            it.tontineId = this.tontineId
            type = SanctionType.LATENESS
            amount = 1000.00
            it.financial = financial
            status = SanctionStatus.CONTESTED
            reason = 'retard'
        }
    }

    def "decide ACCEPT : passe en CANCELLED + refund si financiere + audit"() {
        given:
        def s = contested(true)
        sanctionRepository.findById(sanctionId) >> Optional.of(s)
        sanctionRepository.save(_) >> { it[0] }

        when:
        def dto = service.decide(sanctionId, tontineId, userId, 'Censeur X',
                new DecideContestationRequest(ContestationDecision.ACCEPT, 'OK'))

        then:
        dto.status == SanctionStatus.CANCELLED
        s.cancelledByUserId == userId
        s.cancelledByFullName == 'Censeur X'
        s.cancelledByRole == SanctionCancelByRole.CENSOR
        s.cancelReason == 'OK'
        s.resolvedByUserId == userId
        s.refundInitiated
        1 * auditService.record(userId, 'SANCTION_CONTESTATION_ACCEPT', 'Sanction',
                sanctionId.toString(), tontineId, { it.contains('OK') })
    }

    def "decide ACCEPT : sanction non financiere n'initie pas de refund"() {
        given:
        def s = contested(false)
        sanctionRepository.findById(sanctionId) >> Optional.of(s)
        sanctionRepository.save(_) >> { it[0] }

        when:
        service.decide(sanctionId, tontineId, userId, 'C',
                new DecideContestationRequest(ContestationDecision.ACCEPT, 'OK'))

        then:
        !s.refundInitiated
    }

    def "decide REJECT : passe en CONFIRMED"() {
        given:
        def s = contested()
        sanctionRepository.findById(sanctionId) >> Optional.of(s)
        sanctionRepository.save(_) >> { it[0] }

        when:
        def dto = service.decide(sanctionId, tontineId, userId, 'C',
                new DecideContestationRequest(ContestationDecision.REJECT, 'preuve OK'))

        then:
        dto.status == SanctionStatus.CONFIRMED
        s.resolvedByUserId == userId
        !s.refundInitiated
        1 * auditService.record(userId, 'SANCTION_CONTESTATION_REJECT', 'Sanction',
                sanctionId.toString(), tontineId, _)
    }

    def "decide TRANSFER_PRESIDENT : ne change pas le status mais ajoute une note d'escalade"() {
        given:
        def s = contested()
        sanctionRepository.findById(sanctionId) >> Optional.of(s)
        sanctionRepository.save(_) >> { it[0] }

        when:
        def dto = service.decide(sanctionId, tontineId, userId, 'C',
                new DecideContestationRequest(ContestationDecision.TRANSFER_PRESIDENT, 'cas complexe'))

        then:
        dto.status == SanctionStatus.CONTESTED   // inchange
        s.cancelReason.contains('Escalated to President')
        s.cancelReason.contains('cas complexe')
        s.resolvedByUserId == userId
        1 * auditService.record(userId, 'SANCTION_TRANSFER_PRESIDENT', 'Sanction',
                sanctionId.toString(), tontineId, _)
    }

    @Unroll
    def "decide : refuse une sanction qui n'est pas CONTESTED (#status)"() {
        given:
        def s = contested().tap { it.status = status }
        sanctionRepository.findById(sanctionId) >> Optional.of(s)

        when:
        service.decide(sanctionId, tontineId, userId, 'C',
                new DecideContestationRequest(ContestationDecision.ACCEPT, 'x'))

        then:
        ApiException ex = thrown()
        ex.code == 'CONTESTATION_INVALID_STATE'

        where:
        status << [SanctionStatus.PENDING, SanctionStatus.CONFIRMED,
                   SanctionStatus.PAID, SanctionStatus.CANCELLED, SanctionStatus.WAIVED]
    }

    def "decide : refuse une sanction d'une autre tontine"() {
        given:
        def s = contested().tap { it.tontineId = UUID.randomUUID() }
        sanctionRepository.findById(sanctionId) >> Optional.of(s)

        when:
        service.decide(sanctionId, tontineId, userId, 'C',
                new DecideContestationRequest(ContestationDecision.ACCEPT, 'x'))

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
    }
}
