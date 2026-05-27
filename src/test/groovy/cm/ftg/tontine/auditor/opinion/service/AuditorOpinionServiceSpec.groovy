package cm.ftg.tontine.auditor.opinion.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.auditor.opinion.dto.EmitOpinionRequest
import cm.ftg.tontine.auditor.security.AuditorAccessChecker
import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.member.entity.Member
import cm.ftg.tontine.president.validation.entity.ValidationItem
import cm.ftg.tontine.president.validation.enums.AuditorOpinionStatus
import cm.ftg.tontine.president.validation.enums.ValidationCategory
import cm.ftg.tontine.president.validation.enums.ValidationStatus
import cm.ftg.tontine.president.validation.repository.ValidationItemRepository
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class AuditorOpinionServiceSpec extends Specification {

    ValidationItemRepository repository = Mock()
    AuditorAccessChecker accessChecker = Mock()
    AuditService auditService = Mock()

    @Subject
    AuditorOpinionService service = new AuditorOpinionService(
            repository, accessChecker, auditService)

    UUID tontineId = UUID.randomUUID()
    UUID userId = UUID.randomUUID()
    UUID validationId = UUID.randomUUID()
    Member auditor = new Member().tap { firstName = 'Audrey'; lastName = 'Tor' }

    def setup() {
        accessChecker.requireAuditor(userId, tontineId) >> auditor
    }

    ValidationItem pending(AuditorOpinionStatus opinion = null) {
        new ValidationItem().tap {
            id = validationId
            it.tontineId = this.tontineId
            status = ValidationStatus.PENDING
            category = ValidationCategory.FINANCIAL_OPERATION
            title = 'Retrait exceptionnel'
            auditorOpinionStatus = opinion
        }
    }

    @Unroll
    def "emitOpinion #status : enregistre le statut + commentaire + auditeur + audit"() {
        given:
        def v = pending()
        repository.findById(validationId) >> Optional.of(v)
        repository.save(_) >> { it[0] }

        when:
        def dto = service.emitOpinion(validationId, tontineId, userId,
                new EmitOpinionRequest(status, 'commentaire'))

        then:
        dto.auditorOpinion() != null
        dto.auditorOpinion().status() == status
        dto.auditorOpinion().comment() == 'commentaire'
        dto.auditorOpinion().userFullName() == 'Audrey Tor'
        v.auditorOpinionComment == 'commentaire'
        v.auditorUserId == userId
        v.auditorFullName == 'Audrey Tor'
        v.auditorEmittedAt != null
        1 * auditService.record(userId, 'VALIDATION_OPINION_EMIT', 'Validation',
                validationId.toString(), tontineId, { it.contains(status.name()) })

        where:
        status << [AuditorOpinionStatus.FAVORABLE, AuditorOpinionStatus.RESERVED,
                   AuditorOpinionStatus.UNFAVORABLE]
    }

    def "emitOpinion : refuse un avis deja emis"() {
        given:
        def v = pending(AuditorOpinionStatus.FAVORABLE)
        repository.findById(validationId) >> Optional.of(v)

        when:
        service.emitOpinion(validationId, tontineId, userId,
                new EmitOpinionRequest(AuditorOpinionStatus.RESERVED, 'change'))

        then:
        ApiException ex = thrown()
        ex.code == 'AUDITOR_OPINION_ALREADY_EMITTED'
        ex.status.value() == 409
        0 * repository.save(_)
    }

    def "emitOpinion : refuse une validation d'une autre tontine"() {
        given:
        def v = pending().tap { it.tontineId = UUID.randomUUID() }
        repository.findById(validationId) >> Optional.of(v)

        when:
        service.emitOpinion(validationId, tontineId, userId,
                new EmitOpinionRequest(AuditorOpinionStatus.FAVORABLE, null))

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
    }

    def "emitOpinion : 404 quand la validation n'existe pas"() {
        given:
        repository.findById(validationId) >> Optional.empty()

        when:
        service.emitOpinion(validationId, tontineId, userId,
                new EmitOpinionRequest(AuditorOpinionStatus.FAVORABLE, null))

        then:
        thrown(cm.ftg.tontine.common.exception.ResourceNotFoundException)
    }
}
