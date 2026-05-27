package cm.ftg.tontine.president.validation.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.notification.service.NotificationService
import cm.ftg.tontine.president.security.PresidentAccessChecker
import cm.ftg.tontine.president.validation.dto.DecisionRequest
import cm.ftg.tontine.president.validation.entity.ValidationItem
import cm.ftg.tontine.president.validation.enums.DecisionType
import cm.ftg.tontine.president.validation.enums.ValidationCategory
import cm.ftg.tontine.president.validation.enums.ValidationStatus
import cm.ftg.tontine.president.validation.repository.ValidationItemRepository
import cm.ftg.tontine.realtime.RealtimeEventPublisher
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class ValidationServiceSpec extends Specification {

    ValidationItemRepository repository = Mock()
    PresidentAccessChecker accessChecker = Mock()
    AuditService auditService = Mock()
    NotificationService notificationService = Mock()
    RealtimeEventPublisher realtime = Mock()

    @Subject
    ValidationService service = new ValidationService(
            repository, accessChecker, auditService, notificationService, realtime)

    UUID tontineId = UUID.randomUUID()
    UUID userId = UUID.randomUUID()
    UUID submitterId = UUID.randomUUID()

    ValidationItem pendingItem() {
        new ValidationItem().tap {
            id = UUID.randomUUID()
            it.tontineId = this.tontineId
            submittedByUserId = submitterId
            status = ValidationStatus.PENDING
            category = ValidationCategory.FINANCIAL_OPERATION
            title = 'Retrait exceptionnel'
        }
    }

    def "decide : refuse de re-statuer sur une demande deja decidee"() {
        given:
        def item = pendingItem().tap { status = ValidationStatus.APPROVED }
        repository.findById(item.id) >> Optional.of(item)

        when:
        service.decide(item.id, tontineId, userId,
                new DecisionRequest(DecisionType.APPROVED, 'ok'))

        then:
        ApiException ex = thrown()
        ex.code == 'VALIDATION_ALREADY_DECIDED'
        ex.status.value() == 409
    }

    def "decide : refuse une tontine non concordante"() {
        given:
        def item = pendingItem().tap { it.tontineId = UUID.randomUUID() }
        repository.findById(item.id) >> Optional.of(item)

        when:
        service.decide(item.id, tontineId, userId,
                new DecisionRequest(DecisionType.APPROVED, null))

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
    }

    @Unroll
    def "decide : transition #decision -> status #expected + notif + broadcast"() {
        given:
        def item = pendingItem()
        repository.findById(item.id) >> Optional.of(item)
        repository.save(_) >> { ValidationItem v -> v }

        when:
        def dto = service.decide(item.id, tontineId, userId,
                new DecisionRequest(decision, 'commentaire'))

        then:
        dto.status == expected
        item.decision == decision
        item.decidedByUserId == userId
        item.decidedAt != null
        1 * auditService.record(userId, 'VALIDATION_DECIDE', 'Validation', item.id.toString(),
                tontineId, { it.contains(decision.name()) })
        1 * notificationService.publish(submitterId, tontineId, _, _,
                'Decision sur votre demande', { it.contains(verb) }, _ as String)
        1 * realtime.toPresidentDashboard(tontineId, 'validation.decided', _ as Map)

        where:
        decision                | expected                     | verb
        DecisionType.APPROVED   | ValidationStatus.APPROVED    | 'approuvee'
        DecisionType.REJECTED   | ValidationStatus.REJECTED    | 'rejetee'
        DecisionType.BLOCKED    | ValidationStatus.BLOCKED     | 'bloquee'
    }
}
