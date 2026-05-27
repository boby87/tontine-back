package cm.ftg.tontine.secretary.rsvp.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.member.entity.Member
import cm.ftg.tontine.member.repository.MemberRepository
import cm.ftg.tontine.president.session.entity.Session
import cm.ftg.tontine.president.session.repository.SessionRepository
import cm.ftg.tontine.secretary.rsvp.dto.UpsertRsvpRequest
import cm.ftg.tontine.secretary.rsvp.entity.SessionRsvp
import cm.ftg.tontine.secretary.rsvp.enums.RsvpStatus
import cm.ftg.tontine.secretary.rsvp.repository.SessionRsvpRepository
import cm.ftg.tontine.secretary.security.SecretaryAccessChecker
import spock.lang.Specification
import spock.lang.Subject

import java.time.Instant

class SessionRsvpServiceSpec extends Specification {

    SessionRsvpRepository rsvpRepository = Mock()
    SessionRepository sessionRepository = Mock()
    MemberRepository memberRepository = Mock()
    SecretaryAccessChecker accessChecker = Mock()
    AuditService auditService = Mock()

    @Subject
    SessionRsvpService service = new SessionRsvpService(
            rsvpRepository, sessionRepository, memberRepository, accessChecker, auditService)

    UUID tontineId = UUID.randomUUID()
    UUID userId = UUID.randomUUID()
    UUID sessionId = UUID.randomUUID()

    Session session(BigDecimal quorum = 60.00) {
        new Session().tap {
            id = sessionId
            it.tontineId = this.tontineId
            number = 3
            scheduledAt = Instant.now().plusSeconds(86400)
            quorumThreshold = quorum
        }
    }

    Member member(String first, String last, UUID id = UUID.randomUUID()) {
        new Member().tap {
            it.id = id
            it.tontineId = this.tontineId
            firstName = first
            lastName = last
        }
    }

    SessionRsvp rsvp(UUID memberId, RsvpStatus status) {
        new SessionRsvp().tap {
            it.id = UUID.randomUUID()
            it.sessionId = this.sessionId
            it.memberId = memberId
            memberFullName = 'X Y'
            it.status = status
        }
    }

    def "summary : compte les statuts et calcule le quorum atteint"() {
        given:
        def s = session(50.00)
        def m1 = member('A', 'A'); def m2 = member('B', 'B')
        def m3 = member('C', 'C'); def m4 = member('D', 'D')
        sessionRepository.findById(sessionId) >> Optional.of(s)
        memberRepository.findByTontineId(tontineId) >> [m1, m2, m3, m4]
        rsvpRepository.findBySessionId(sessionId) >> [
                rsvp(m1.id, RsvpStatus.CONFIRMED),
                rsvp(m2.id, RsvpStatus.CONFIRMED),
                rsvp(m3.id, RsvpStatus.DECLINED)
        ]

        when:
        def result = service.summary(sessionId, tontineId, userId)

        then:
        result.totalMembers() == 4
        result.confirmed() == 2
        result.declined() == 1
        result.tentative() == 0
        result.pending() == 1   // m4 sans RSVP
        result.quorumPercent() == 50.00
        result.quorumReached()  // 2/4 = 50% >= 50%
        result.rsvps().size() == 4
    }

    def "summary : quorum non atteint quand confirmes en dessous du seuil"() {
        given:
        def s = session(75.00)
        def m1 = member('A', 'A'); def m2 = member('B', 'B')
        sessionRepository.findById(sessionId) >> Optional.of(s)
        memberRepository.findByTontineId(tontineId) >> [m1, m2]
        rsvpRepository.findBySessionId(sessionId) >> [rsvp(m1.id, RsvpStatus.CONFIRMED)]

        when:
        def result = service.summary(sessionId, tontineId, userId)

        then:
        result.confirmed() == 1
        !result.quorumReached()   // 1/2 = 50% < 75%
    }

    def "summary : refuse une session d'une autre tontine"() {
        given:
        def s = session().tap { it.tontineId = UUID.randomUUID() }
        sessionRepository.findById(sessionId) >> Optional.of(s)

        when:
        service.summary(sessionId, tontineId, userId)

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
    }

    def "upsert : cree un nouveau RSVP quand inexistant"() {
        given:
        def s = session()
        def m = member('Jean', 'Dupont')
        sessionRepository.findById(sessionId) >> Optional.of(s)
        memberRepository.findById(m.id) >> Optional.of(m)
        rsvpRepository.findBySessionIdAndMemberId(sessionId, m.id) >> Optional.empty()
        rsvpRepository.save(_) >> { SessionRsvp r -> r.id = UUID.randomUUID(); r }

        when:
        def dto = service.upsert(sessionId, m.id, tontineId, userId,
                new UpsertRsvpRequest(RsvpStatus.CONFIRMED, null))

        then:
        dto.status == RsvpStatus.CONFIRMED
        dto.memberFullName == 'Jean Dupont'
        1 * auditService.record(userId, 'RSVP_UPSERT', 'SessionRsvp', _, tontineId,
                { it.contains('CONFIRMED') })
    }

    def "upsert : met a jour un RSVP existant"() {
        given:
        def s = session()
        def m = member('Marie', 'Curie')
        def existing = rsvp(m.id, RsvpStatus.PENDING)
        sessionRepository.findById(sessionId) >> Optional.of(s)
        memberRepository.findById(m.id) >> Optional.of(m)
        rsvpRepository.findBySessionIdAndMemberId(sessionId, m.id) >> Optional.of(existing)
        rsvpRepository.save(_) >> { it[0] }

        when:
        def dto = service.upsert(sessionId, m.id, tontineId, userId,
                new UpsertRsvpRequest(RsvpStatus.DECLINED, 'voyage'))

        then:
        existing.status == RsvpStatus.DECLINED
        existing.reason == 'voyage'
        existing.respondedAt != null
        dto.status == RsvpStatus.DECLINED
    }

    def "upsert : refuse un membre d'une autre tontine"() {
        given:
        def s = session()
        def m = member('X', 'Y').tap { it.tontineId = UUID.randomUUID() }
        sessionRepository.findById(sessionId) >> Optional.of(s)
        memberRepository.findById(m.id) >> Optional.of(m)

        when:
        service.upsert(sessionId, m.id, tontineId, userId,
                new UpsertRsvpRequest(RsvpStatus.CONFIRMED, null))

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
    }

    def "remind : compte les membres sans reponse ou PENDING"() {
        given:
        def s = session()
        def m1 = member('A', 'A'); def m2 = member('B', 'B'); def m3 = member('C', 'C')
        sessionRepository.findById(sessionId) >> Optional.of(s)
        memberRepository.findByTontineId(tontineId) >> [m1, m2, m3]
        rsvpRepository.findBySessionId(sessionId) >> [
                rsvp(m1.id, RsvpStatus.CONFIRMED),
                rsvp(m2.id, RsvpStatus.PENDING)
        ]

        when:
        def result = service.remind(sessionId, tontineId, userId)

        then:
        result.remindedCount() == 2   // m2 PENDING + m3 sans RSVP
        1 * auditService.record(userId, 'RSVP_REMIND', 'Session', _, tontineId,
                { it.contains('"remindedCount":2') })
    }
}
