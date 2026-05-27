package cm.ftg.tontine.censor.justification.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.censor.justification.dto.DecideJustificationRequest
import cm.ftg.tontine.censor.justification.entity.AbsenceJustification
import cm.ftg.tontine.censor.justification.enums.JustificationDecision
import cm.ftg.tontine.censor.justification.enums.JustificationStatus
import cm.ftg.tontine.censor.justification.repository.AbsenceJustificationRepository
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

class AbsenceJustificationCensorServiceSpec extends Specification {

    AbsenceJustificationRepository repository = Mock()
    SanctionRepository sanctionRepository = Mock()
    CensorAccessChecker accessChecker = Mock()
    AuditService auditService = Mock()

    @Subject
    AbsenceJustificationCensorService service = new AbsenceJustificationCensorService(
            repository, sanctionRepository, accessChecker, auditService)

    UUID tontineId = UUID.randomUUID()
    UUID userId = UUID.randomUUID()
    UUID justificationId = UUID.randomUUID()
    UUID memberId = UUID.randomUUID()
    UUID sessionId = UUID.randomUUID()

    AbsenceJustification justification(JustificationStatus status = JustificationStatus.PENDING_CENSOR) {
        new AbsenceJustification().tap {
            id = justificationId
            it.tontineId = this.tontineId
            it.memberId = this.memberId
            it.sessionId = this.sessionId
            it.status = status
            reason = 'maladie'
        }
    }

    @Unroll
    def "decide #decision : passe en #expected"() {
        given:
        def j = justification()
        repository.findById(justificationId) >> Optional.of(j)
        repository.save(_) >> { it[0] }

        when:
        def dto = service.decide(justificationId, tontineId, userId,
                new DecideJustificationRequest(decision, 'mon avis'))

        then:
        dto.status == expected
        j.censorComment == 'mon avis'
        j.censorDecidedAt != null
        1 * auditService.record(userId, auditAction, 'AbsenceJustification',
                justificationId.toString(), tontineId, { it.contains('mon avis') })

        where:
        decision                              | expected                                    | auditAction
        JustificationDecision.VALIDATE        | JustificationStatus.PENDING_PRESIDENT       | 'JUSTIFICATION_VALIDATE_CENSOR'
        JustificationDecision.REJECT          | JustificationStatus.REJECTED_CENSOR         | 'JUSTIFICATION_REJECT_CENSOR'
        JustificationDecision.REQUEST_INFO    | JustificationStatus.INFO_REQUESTED          | 'JUSTIFICATION_REQUEST_INFO'
    }

    @Unroll
    def "decide : refuse depuis l'etat final #status"() {
        given:
        def j = justification(status)
        repository.findById(justificationId) >> Optional.of(j)

        when:
        service.decide(justificationId, tontineId, userId,
                new DecideJustificationRequest(JustificationDecision.VALIDATE, 'c'))

        then:
        ApiException ex = thrown()
        ex.code == 'JUSTIFICATION_INVALID_STATE'
        ex.status.value() == 409

        where:
        status << [JustificationStatus.APPROVED, JustificationStatus.REJECTED_CENSOR,
                   JustificationStatus.REJECTED_PRESIDENT, JustificationStatus.PENDING_PRESIDENT]
    }

    def "decide : refuse une justification d'une autre tontine"() {
        given:
        def j = justification().tap { it.tontineId = UUID.randomUUID() }
        repository.findById(justificationId) >> Optional.of(j)

        when:
        service.decide(justificationId, tontineId, userId,
                new DecideJustificationRequest(JustificationDecision.VALIDATE, 'c'))

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
    }

    def "presidentApprove : passe APPROVED et annule la sanction auto liee"() {
        given:
        def j = justification(JustificationStatus.PENDING_PRESIDENT)
        def autoSanction = new Sanction().tap {
            id = UUID.randomUUID()
            it.tontineId = this.tontineId
            it.memberId = this.memberId
            it.sessionId = this.sessionId
            type = SanctionType.ABSENCE
            amount = 5000.00
            financial = true
            autoDetected = true
            status = SanctionStatus.PENDING
        }
        repository.findById(justificationId) >> Optional.of(j)
        repository.save(_) >> { it[0] }
        sanctionRepository.findFirstByTontineIdAndSessionIdAndMemberIdAndAutoDetectedTrueAndStatus(
                tontineId, sessionId, memberId, SanctionStatus.PENDING) >> Optional.of(autoSanction)
        sanctionRepository.save(_) >> { it[0] }

        when:
        def dto = service.presidentApprove(justificationId, tontineId, userId)

        then:
        dto.status == JustificationStatus.APPROVED
        j.presidentDecidedAt != null
        j.linkedSanctionId == autoSanction.id
        autoSanction.status == SanctionStatus.CANCELLED
        autoSanction.cancelledByRole == SanctionCancelByRole.PRESIDENT
        autoSanction.refundInitiated
        1 * auditService.record(userId, 'JUSTIFICATION_PRESIDENT_APPROVE',
                'AbsenceJustification', justificationId.toString(), tontineId, null)
    }

    def "presidentApprove : ne plante pas si aucune sanction auto liee"() {
        given:
        def j = justification(JustificationStatus.PENDING_PRESIDENT)
        repository.findById(justificationId) >> Optional.of(j)
        repository.save(_) >> { it[0] }
        sanctionRepository.findFirstByTontineIdAndSessionIdAndMemberIdAndAutoDetectedTrueAndStatus(
                _, _, _, _) >> Optional.empty()

        when:
        def dto = service.presidentApprove(justificationId, tontineId, userId)

        then:
        dto.status == JustificationStatus.APPROVED
        j.linkedSanctionId == null
        0 * sanctionRepository.save(_)
    }

    def "presidentApprove : refuse si la justification n'est pas en PENDING_PRESIDENT"() {
        given:
        def j = justification(JustificationStatus.PENDING_CENSOR)
        repository.findById(justificationId) >> Optional.of(j)

        when:
        service.presidentApprove(justificationId, tontineId, userId)

        then:
        ApiException ex = thrown()
        ex.code == 'JUSTIFICATION_INVALID_STATE'
    }
}
