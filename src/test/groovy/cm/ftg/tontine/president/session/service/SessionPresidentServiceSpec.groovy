package cm.ftg.tontine.president.session.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.president.security.PresidentAccessChecker
import cm.ftg.tontine.president.session.dto.CloseSessionRequest
import cm.ftg.tontine.president.session.entity.AgendaItem
import cm.ftg.tontine.president.session.entity.Session
import cm.ftg.tontine.president.session.enums.AgendaItemStatus
import cm.ftg.tontine.president.session.enums.SessionStatus
import cm.ftg.tontine.president.session.repository.AgendaItemRepository
import cm.ftg.tontine.president.session.repository.SessionAttendanceRepository
import cm.ftg.tontine.president.session.repository.SessionRepository
import cm.ftg.tontine.realtime.RealtimeEventPublisher
import cm.ftg.tontine.tontine.repository.TontineRepository
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.time.Instant

class SessionPresidentServiceSpec extends Specification {

    SessionRepository sessionRepository = Mock()
    AgendaItemRepository agendaItemRepository = Mock()
    SessionAttendanceRepository attendanceRepository = Mock()
    TontineRepository tontineRepository = Mock()
    PresidentAccessChecker accessChecker = Mock()
    AuditService auditService = Mock()
    RealtimeEventPublisher realtime = Mock()

    @Subject
    SessionPresidentService service = new SessionPresidentService(
            sessionRepository, agendaItemRepository, attendanceRepository, tontineRepository,
            accessChecker, auditService, realtime)

    UUID tontineId = UUID.randomUUID()
    UUID userId = UUID.randomUUID()
    UUID sessionId = UUID.randomUUID()

    Session session(SessionStatus status = SessionStatus.SCHEDULED) {
        new Session().tap {
            id = sessionId
            it.tontineId = this.tontineId
            number = 7
            it.status = status
        }
    }

    AgendaItem agenda(int idx, AgendaItemStatus status = AgendaItemStatus.PENDING) {
        new AgendaItem().tap {
            id = UUID.randomUUID()
            it.sessionId = this.sessionId
            orderIdx = idx
            title = "Point #${idx}"
            it.status = status
        }
    }

    private void emptyBuildDetailedStubs() {
        agendaItemRepository.findBySessionIdOrderByOrderIdxAsc(_) >> []
        attendanceRepository.findBySessionIdOrderByFullNameAsc(_) >> []
    }

    def "open : passe SCHEDULED -> IN_PROGRESS et broadcast session.opened"() {
        given:
        def s = session(SessionStatus.SCHEDULED)
        sessionRepository.findById(sessionId) >> Optional.of(s)
        sessionRepository.save(_) >> { it[0] }
        emptyBuildDetailedStubs()

        when:
        def dto = service.open(sessionId, tontineId, userId)

        then:
        dto.status == SessionStatus.IN_PROGRESS
        s.startedAt != null
        1 * auditService.record(userId, 'SESSION_OPEN', 'Session', sessionId.toString(), tontineId, null)
        1 * realtime.toSession(tontineId, sessionId, 'session.opened', _ as Map)
    }

    @Unroll
    def "open : refuse un statut non-SCHEDULED (#status)"() {
        given:
        def s = session(status)
        sessionRepository.findById(sessionId) >> Optional.of(s)

        when:
        service.open(sessionId, tontineId, userId)

        then:
        ApiException ex = thrown()
        ex.code == 'SESSION_INVALID_STATE'
        ex.status.value() == 409
        0 * realtime.toSession(_, _, _, _)

        where:
        status << [SessionStatus.IN_PROGRESS, SessionStatus.COMPLETED, SessionStatus.CANCELLED]
    }

    def "open : refuse une session d'une autre tontine"() {
        given:
        def s = session().tap { it.tontineId = UUID.randomUUID() }
        sessionRepository.findById(sessionId) >> Optional.of(s)

        when:
        service.open(sessionId, tontineId, userId)

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
    }

    def "advanceAgenda : marque l'item courant DONE et active le suivant"() {
        given:
        def s = session(SessionStatus.IN_PROGRESS)
        def a1 = agenda(0, AgendaItemStatus.IN_PROGRESS)
        def a2 = agenda(1, AgendaItemStatus.PENDING)
        def a3 = agenda(2, AgendaItemStatus.PENDING)
        sessionRepository.findById(sessionId) >> Optional.of(s)
        agendaItemRepository.findBySessionIdOrderByOrderIdxAsc(sessionId) >> [a1, a2, a3]
        agendaItemRepository.save(_) >> { it[0] }
        attendanceRepository.findBySessionIdOrderByFullNameAsc(_) >> []

        when:
        service.advanceAgenda(sessionId, a1.id, tontineId, userId)

        then:
        a1.status == AgendaItemStatus.DONE
        a2.status == AgendaItemStatus.IN_PROGRESS
        a3.status == AgendaItemStatus.PENDING
        1 * realtime.toSession(tontineId, sessionId, 'agenda.advanced', _ as Map)
    }

    def "advanceAgenda : refuse si la session n'est pas IN_PROGRESS"() {
        given:
        def s = session(SessionStatus.SCHEDULED)
        sessionRepository.findById(sessionId) >> Optional.of(s)

        when:
        service.advanceAgenda(sessionId, UUID.randomUUID(), tontineId, userId)

        then:
        ApiException ex = thrown()
        ex.code == 'SESSION_NOT_IN_PROGRESS'
    }

    def "advanceAgenda : 404 si l'item d'agenda n'existe pas"() {
        given:
        def s = session(SessionStatus.IN_PROGRESS)
        def a1 = agenda(0)
        sessionRepository.findById(sessionId) >> Optional.of(s)
        agendaItemRepository.findBySessionIdOrderByOrderIdxAsc(sessionId) >> [a1]

        when:
        service.advanceAgenda(sessionId, UUID.randomUUID(), tontineId, userId)

        then:
        thrown(cm.ftg.tontine.common.exception.ResourceNotFoundException)
    }

    def "signCagnotte : pose le flag et broadcast cagnotte.signed"() {
        given:
        def s = session(SessionStatus.IN_PROGRESS).tap {
            beneficiaryMemberId = UUID.randomUUID()
            cagnotteAmount = 50000.00
        }
        sessionRepository.findById(sessionId) >> Optional.of(s)
        sessionRepository.save(_) >> { it[0] }
        emptyBuildDetailedStubs()

        when:
        def dto = service.signCagnotte(sessionId, tontineId, userId)

        then:
        s.cagnotteSignedByPresident
        dto.cagnotteSignedByPresident
        1 * realtime.toSession(tontineId, sessionId, 'cagnotte.signed', _ as Map)
    }

    def "signCagnotte : refuse hors IN_PROGRESS"() {
        given:
        def s = session(SessionStatus.SCHEDULED)
        sessionRepository.findById(sessionId) >> Optional.of(s)

        when:
        service.signCagnotte(sessionId, tontineId, userId)

        then:
        ApiException ex = thrown()
        ex.code == 'SESSION_NOT_IN_PROGRESS'
    }

    def "close : passe IN_PROGRESS -> COMPLETED, pose endedAt et nextSessionDate"() {
        given:
        def s = session(SessionStatus.IN_PROGRESS)
        sessionRepository.findById(sessionId) >> Optional.of(s)
        sessionRepository.save(_) >> { it[0] }
        emptyBuildDetailedStubs()
        def next = Instant.now().plusSeconds(7 * 86400)

        when:
        def dto = service.close(sessionId, tontineId, userId, new CloseSessionRequest(next))

        then:
        dto.status == SessionStatus.COMPLETED
        s.endedAt != null
        s.nextSessionDate == next
        1 * realtime.toSession(tontineId, sessionId, 'session.closed', _ as Map)
    }

    def "close : sans nextSessionDate ne modifie pas la date suivante"() {
        given:
        def s = session(SessionStatus.IN_PROGRESS).tap { nextSessionDate = null }
        sessionRepository.findById(sessionId) >> Optional.of(s)
        sessionRepository.save(_) >> { it[0] }
        emptyBuildDetailedStubs()

        when:
        service.close(sessionId, tontineId, userId, new CloseSessionRequest(null))

        then:
        s.status == SessionStatus.COMPLETED
        s.nextSessionDate == null
    }

    def "close : refuse hors IN_PROGRESS"() {
        given:
        def s = session(SessionStatus.SCHEDULED)
        sessionRepository.findById(sessionId) >> Optional.of(s)

        when:
        service.close(sessionId, tontineId, userId, new CloseSessionRequest(null))

        then:
        ApiException ex = thrown()
        ex.code == 'SESSION_NOT_IN_PROGRESS'
    }
}
