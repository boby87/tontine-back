package cm.ftg.tontine.secretary.attendance.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.member.entity.Member
import cm.ftg.tontine.member.repository.MemberRepository
import cm.ftg.tontine.president.session.entity.Session
import cm.ftg.tontine.president.session.entity.SessionAttendanceEntry
import cm.ftg.tontine.president.session.enums.AttendanceStatus
import cm.ftg.tontine.president.session.repository.SessionAttendanceRepository
import cm.ftg.tontine.president.session.repository.SessionRepository
import cm.ftg.tontine.secretary.attendance.dto.UpsertAttendanceRequest
import cm.ftg.tontine.secretary.security.SecretaryAccessChecker
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class AttendanceSecretaryServiceSpec extends Specification {

    SessionAttendanceRepository attendanceRepository = Mock()
    SessionRepository sessionRepository = Mock()
    MemberRepository memberRepository = Mock()
    SecretaryAccessChecker accessChecker = Mock()
    AuditService auditService = Mock()

    @Subject
    AttendanceSecretaryService service = new AttendanceSecretaryService(
            attendanceRepository, sessionRepository, memberRepository, accessChecker, auditService)

    UUID tontineId = UUID.randomUUID()
    UUID userId = UUID.randomUUID()
    UUID sessionId = UUID.randomUUID()

    Session session() {
        new Session().tap { id = sessionId; it.tontineId = this.tontineId }
    }

    Member member(String first = 'Anne', String last = 'Doe') {
        new Member().tap {
            id = UUID.randomUUID()
            it.tontineId = this.tontineId
            firstName = first
            lastName = last
        }
    }

    @Unroll
    def "upsert : pose checkInAt sur #status quand approprie"() {
        given:
        def s = session()
        def m = member()
        sessionRepository.findById(sessionId) >> Optional.of(s)
        memberRepository.findById(m.id) >> Optional.of(m)
        attendanceRepository.findBySessionIdAndMemberId(sessionId, m.id) >> Optional.empty()
        attendanceRepository.save(_) >> { SessionAttendanceEntry e -> e.id = UUID.randomUUID(); e }

        when:
        def dto = service.upsert(sessionId, m.id, tontineId, userId,
                new UpsertAttendanceRequest(status))

        then:
        dto.status == status
        (checkInExpected) == (dto.checkInAt != null)

        where:
        status                    | checkInExpected
        AttendanceStatus.PRESENT  | true
        AttendanceStatus.LATE     | true
        AttendanceStatus.ABSENT   | false
        AttendanceStatus.EXCUSED  | false
    }

    def "upsert : met a jour une entree existante"() {
        given:
        def s = session()
        def m = member()
        def existing = new SessionAttendanceEntry().tap {
            id = UUID.randomUUID()
            it.sessionId = this.sessionId
            it.memberId = m.id
            status = AttendanceStatus.ABSENT
            fullName = 'Anne Doe'
        }
        sessionRepository.findById(sessionId) >> Optional.of(s)
        memberRepository.findById(m.id) >> Optional.of(m)
        attendanceRepository.findBySessionIdAndMemberId(sessionId, m.id) >> Optional.of(existing)
        attendanceRepository.save(_) >> { it[0] }

        when:
        def dto = service.upsert(sessionId, m.id, tontineId, userId,
                new UpsertAttendanceRequest(AttendanceStatus.PRESENT))

        then:
        existing.status == AttendanceStatus.PRESENT
        existing.checkInAt != null
        dto.status == AttendanceStatus.PRESENT
        1 * auditService.record(userId, 'ATTENDANCE_UPSERT', 'SessionAttendanceEntry', _, tontineId,
                { it.contains('PRESENT') })
    }

    def "upsert : refuse un membre d'une autre tontine"() {
        given:
        def s = session()
        def m = member().tap { it.tontineId = UUID.randomUUID() }
        sessionRepository.findById(sessionId) >> Optional.of(s)
        memberRepository.findById(m.id) >> Optional.of(m)

        when:
        service.upsert(sessionId, m.id, tontineId, userId,
                new UpsertAttendanceRequest(AttendanceStatus.PRESENT))

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
        0 * attendanceRepository.save(_)
    }

    def "finalize : cree des entrees ABSENT pour les membres sans enregistrement"() {
        given:
        def s = session()
        def m1 = member('A', 'A'); def m2 = member('B', 'B'); def m3 = member('C', 'C')
        sessionRepository.findById(sessionId) >> Optional.of(s)
        memberRepository.findByTontineId(tontineId) >> [m1, m2, m3]
        // seul m1 a une entree
        def existing = new SessionAttendanceEntry().tap {
            id = UUID.randomUUID()
            it.sessionId = sessionId
            it.memberId = m1.id
            status = AttendanceStatus.PRESENT
        }
        attendanceRepository.findBySessionId(sessionId) >> [existing]

        when:
        service.finalize(sessionId, tontineId, userId)

        then:
        2 * attendanceRepository.save({ SessionAttendanceEntry e ->
            e.status == AttendanceStatus.ABSENT && (e.memberId == m2.id || e.memberId == m3.id)
        })
        1 * auditService.record(userId, 'ATTENDANCE_FINALIZE', 'Session', _, tontineId,
                { it.contains('"absentCreated":2') })
    }

    def "finalize : ne cree aucune entree quand tous les membres sont enregistres"() {
        given:
        def s = session()
        def m1 = member(); def m2 = member()
        sessionRepository.findById(sessionId) >> Optional.of(s)
        memberRepository.findByTontineId(tontineId) >> [m1, m2]
        attendanceRepository.findBySessionId(sessionId) >> [
                new SessionAttendanceEntry().tap { it.memberId = m1.id; status = AttendanceStatus.PRESENT },
                new SessionAttendanceEntry().tap { it.memberId = m2.id; status = AttendanceStatus.LATE }
        ]

        when:
        service.finalize(sessionId, tontineId, userId)

        then:
        0 * attendanceRepository.save(_)
        1 * auditService.record(userId, 'ATTENDANCE_FINALIZE', _, _, _,
                { it.contains('"absentCreated":0') })
    }
}
