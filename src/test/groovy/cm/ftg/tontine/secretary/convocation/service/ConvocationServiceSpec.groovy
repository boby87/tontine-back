package cm.ftg.tontine.secretary.convocation.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.president.session.entity.Session
import cm.ftg.tontine.president.session.repository.SessionRepository
import cm.ftg.tontine.secretary.convocation.dto.CreateConvocationRequest
import cm.ftg.tontine.secretary.convocation.dto.CreateReminderRequest
import cm.ftg.tontine.secretary.convocation.entity.Convocation
import cm.ftg.tontine.secretary.convocation.enums.ConvocationChannel
import cm.ftg.tontine.secretary.convocation.enums.ConvocationStatus
import cm.ftg.tontine.secretary.convocation.repository.ConvocationRepository
import cm.ftg.tontine.secretary.security.SecretaryAccessChecker
import spock.lang.Specification
import spock.lang.Subject

import java.time.Instant

class ConvocationServiceSpec extends Specification {

    ConvocationRepository convocationRepository = Mock()
    SessionRepository sessionRepository = Mock()
    SecretaryAccessChecker accessChecker = Mock()
    AuditService auditService = Mock()

    @Subject
    ConvocationService service = new ConvocationService(
            convocationRepository, sessionRepository, accessChecker, auditService)

    UUID tontineId = UUID.randomUUID()
    UUID userId = UUID.randomUUID()
    UUID sessionId = UUID.randomUUID()

    Session validSession() {
        new Session().tap {
            id = sessionId
            it.tontineId = this.tontineId
            number = 5
            scheduledAt = Instant.now().plusSeconds(7200)
        }
    }

    def "create : passage en SENT quand scheduledAt absent (envoi immediat)"() {
        given:
        def s = validSession()
        sessionRepository.findById(sessionId) >> Optional.of(s)
        convocationRepository.save(_) >> { Convocation c -> c.id = UUID.randomUUID(); c }
        def req = new CreateConvocationRequest(
                sessionId,
                [ConvocationChannel.SMS, ConvocationChannel.IN_APP] as Set,
                [UUID.randomUUID(), UUID.randomUUID()] as Set,
                false,
                'Convocation pour la session #5',
                [new CreateReminderRequest(24), new CreateReminderRequest(2)],
                null
        )

        when:
        def dto = service.create(tontineId, userId, req)

        then:
        dto.status == ConvocationStatus.SENT
        dto.sentAt != null
        dto.scheduledAt == null
        dto.totalRecipients == 2
        dto.reminders.size() == 2
        1 * auditService.record(userId, 'CONVOCATION_CREATE', 'Convocation', _, tontineId,
                { it.contains('SENT') })
    }

    def "create : passage en SCHEDULED quand scheduledAt dans le futur"() {
        given:
        def s = validSession()
        sessionRepository.findById(sessionId) >> Optional.of(s)
        convocationRepository.save(_) >> { Convocation c -> c.id = UUID.randomUUID(); c }
        def futureAt = Instant.now().plusSeconds(3600)
        def req = new CreateConvocationRequest(
                sessionId,
                [ConvocationChannel.EMAIL] as Set,
                [UUID.randomUUID()] as Set,
                true,
                'Convocation programmee',
                null,
                futureAt
        )

        when:
        def dto = service.create(tontineId, userId, req)

        then:
        dto.status == ConvocationStatus.SCHEDULED
        dto.scheduledAt == futureAt
        dto.sentAt == null
        dto.includeCandidates
    }

    def "create : envoi immediat quand scheduledAt est dans le passe"() {
        given:
        def s = validSession()
        sessionRepository.findById(sessionId) >> Optional.of(s)
        convocationRepository.save(_) >> { Convocation c -> c.id = UUID.randomUUID(); c }
        def req = new CreateConvocationRequest(
                sessionId,
                [ConvocationChannel.SMS] as Set,
                [UUID.randomUUID()] as Set,
                false,
                'msg',
                null,
                Instant.now().minusSeconds(60)
        )

        when:
        def dto = service.create(tontineId, userId, req)

        then:
        dto.status == ConvocationStatus.SENT
    }

    def "create : refuse une session d'une autre tontine"() {
        given:
        def s = validSession().tap { it.tontineId = UUID.randomUUID() }
        sessionRepository.findById(sessionId) >> Optional.of(s)
        def req = new CreateConvocationRequest(
                sessionId,
                [ConvocationChannel.SMS] as Set,
                [UUID.randomUUID()] as Set,
                false,
                'msg',
                null,
                null
        )

        when:
        service.create(tontineId, userId, req)

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
        0 * convocationRepository.save(_)
    }
}
