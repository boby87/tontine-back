package cm.ftg.tontine.censor.attendance.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.censor.attendance.dto.DecideAttendanceModificationRequest
import cm.ftg.tontine.censor.attendance.entity.AttendanceModificationRequest
import cm.ftg.tontine.censor.attendance.enums.AttendanceModificationDecision
import cm.ftg.tontine.censor.attendance.enums.AttendanceModificationStatus
import cm.ftg.tontine.censor.attendance.repository.AttendanceModificationRequestRepository
import cm.ftg.tontine.censor.security.CensorAccessChecker
import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.president.sanction.entity.Sanction
import cm.ftg.tontine.president.sanction.enums.SanctionCancelByRole
import cm.ftg.tontine.president.sanction.enums.SanctionStatus
import cm.ftg.tontine.president.sanction.repository.SanctionRepository
import cm.ftg.tontine.president.session.entity.SessionAttendanceEntry
import cm.ftg.tontine.president.session.enums.AttendanceStatus
import cm.ftg.tontine.president.session.repository.SessionAttendanceRepository
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class AttendanceModificationCensorServiceSpec extends Specification {

    AttendanceModificationRequestRepository repository = Mock()
    SessionAttendanceRepository attendanceRepository = Mock()
    SanctionRepository sanctionRepository = Mock()
    CensorAccessChecker accessChecker = Mock()
    AuditService auditService = Mock()

    @Subject
    AttendanceModificationCensorService service = new AttendanceModificationCensorService(
            repository, attendanceRepository, sanctionRepository, accessChecker, auditService)

    UUID tontineId = UUID.randomUUID()
    UUID userId = UUID.randomUUID()
    UUID requestId = UUID.randomUUID()
    UUID memberId = UUID.randomUUID()
    UUID sessionId = UUID.randomUUID()

    AttendanceModificationRequest request(AttendanceStatus from, AttendanceStatus to,
                                          AttendanceModificationStatus status = AttendanceModificationStatus.PENDING) {
        new AttendanceModificationRequest().tap {
            id = requestId
            it.tontineId = this.tontineId
            it.memberId = this.memberId
            it.sessionId = this.sessionId
            memberFullName = 'X Y'
            fromStatus = from
            toStatus = to
            it.status = status
        }
    }

    Sanction autoSanction() {
        new Sanction().tap {
            id = UUID.randomUUID()
            it.tontineId = this.tontineId
            it.memberId = this.memberId
            it.sessionId = this.sessionId
            financial = true
            autoDetected = true
            status = SanctionStatus.PENDING
        }
    }

    def "decide APPROVE : modifie l'entree d'attendance et annule la sanction auto"() {
        given:
        def r = request(AttendanceStatus.ABSENT, AttendanceStatus.PRESENT)
        def existingEntry = new SessionAttendanceEntry().tap {
            id = UUID.randomUUID()
            it.sessionId = this.sessionId
            it.memberId = this.memberId
            status = AttendanceStatus.ABSENT
            fullName = 'X Y'
        }
        def auto = autoSanction()
        repository.findById(requestId) >> Optional.of(r)
        repository.save(_) >> { it[0] }
        attendanceRepository.findBySessionIdAndMemberId(sessionId, memberId) >> Optional.of(existingEntry)
        attendanceRepository.save(_) >> { it[0] }
        sanctionRepository.findFirstByTontineIdAndSessionIdAndMemberIdAndAutoDetectedTrueAndStatus(
                tontineId, sessionId, memberId, SanctionStatus.PENDING) >> Optional.of(auto)
        sanctionRepository.save(_) >> { it[0] }

        when:
        def dto = service.decide(requestId, tontineId, userId,
                new DecideAttendanceModificationRequest(
                        AttendanceModificationDecision.APPROVE, 'OK', null))

        then:
        dto.status == AttendanceModificationStatus.APPROVED
        existingEntry.status == AttendanceStatus.PRESENT
        auto.status == SanctionStatus.CANCELLED
        auto.cancelledByRole == SanctionCancelByRole.CENSOR
        auto.refundInitiated
        r.linkedSanctionId == auto.id
        1 * auditService.record(userId, 'ATTENDANCE_MODIFICATION_APPROVE',
                'AttendanceModificationRequest', requestId.toString(), tontineId, _)
    }

    def "decide APPROVE : cree une entree d'attendance si inexistante"() {
        given:
        def r = request(AttendanceStatus.ABSENT, AttendanceStatus.EXCUSED)
        repository.findById(requestId) >> Optional.of(r)
        repository.save(_) >> { it[0] }
        attendanceRepository.findBySessionIdAndMemberId(sessionId, memberId) >> Optional.empty()
        attendanceRepository.save(_) >> { it[0] }
        sanctionRepository.findFirstByTontineIdAndSessionIdAndMemberIdAndAutoDetectedTrueAndStatus(
                _, _, _, _) >> Optional.empty()

        when:
        service.decide(requestId, tontineId, userId,
                new DecideAttendanceModificationRequest(
                        AttendanceModificationDecision.APPROVE, null, null))

        then:
        1 * attendanceRepository.save({ SessionAttendanceEntry e ->
            e.sessionId == sessionId && e.memberId == memberId &&
                    e.status == AttendanceStatus.EXCUSED
        })
    }

    @Unroll
    def "decide APPROVE : n'annule la sanction que si la transition retire le declencheur (from=#from to=#to => cancels=#cancels)"() {
        given:
        def r = request(from, to)
        def auto = autoSanction()
        repository.findById(requestId) >> Optional.of(r)
        repository.save(_) >> { it[0] }
        attendanceRepository.findBySessionIdAndMemberId(_, _) >> Optional.empty()
        attendanceRepository.save(_) >> { it[0] }
        sanctionRepository.findFirstByTontineIdAndSessionIdAndMemberIdAndAutoDetectedTrueAndStatus(
                _, _, _, _) >> Optional.of(auto)
        sanctionRepository.save(_) >> { it[0] }

        when:
        service.decide(requestId, tontineId, userId,
                new DecideAttendanceModificationRequest(
                        AttendanceModificationDecision.APPROVE, null, null))

        then:
        (cancels ? SanctionStatus.CANCELLED : SanctionStatus.PENDING) == auto.status

        where:
        from                       | to                          | cancels
        AttendanceStatus.ABSENT    | AttendanceStatus.PRESENT    | true
        AttendanceStatus.LATE      | AttendanceStatus.PRESENT    | true
        AttendanceStatus.PRESENT   | AttendanceStatus.ABSENT     | false   // ajout d'un declencheur : pas d'annulation
        AttendanceStatus.ABSENT    | AttendanceStatus.LATE       | false   // reste un declencheur
    }

    def "decide REJECT : passe en REJECTED sans toucher l'attendance ni les sanctions"() {
        given:
        def r = request(AttendanceStatus.ABSENT, AttendanceStatus.PRESENT)
        repository.findById(requestId) >> Optional.of(r)
        repository.save(_) >> { it[0] }

        when:
        def dto = service.decide(requestId, tontineId, userId,
                new DecideAttendanceModificationRequest(
                        AttendanceModificationDecision.REJECT, 'preuve insuffisante', null))

        then:
        dto.status == AttendanceModificationStatus.REJECTED
        r.decisionComment == 'preuve insuffisante'
        r.decidedAt != null
        0 * attendanceRepository.save(_)
        0 * sanctionRepository.save(_)
        1 * auditService.record(userId, 'ATTENDANCE_MODIFICATION_REJECT',
                'AttendanceModificationRequest', requestId.toString(), tontineId, _)
    }

    def "decide REQUEST_INFO : passe en INFO_REQUESTED avec une question"() {
        given:
        def r = request(AttendanceStatus.ABSENT, AttendanceStatus.PRESENT)
        repository.findById(requestId) >> Optional.of(r)
        repository.save(_) >> { it[0] }

        when:
        def dto = service.decide(requestId, tontineId, userId,
                new DecideAttendanceModificationRequest(
                        AttendanceModificationDecision.REQUEST_INFO, 'precisez', 'photo signature ?'))

        then:
        dto.status == AttendanceModificationStatus.INFO_REQUESTED
        r.infoRequest == 'photo signature ?'
        1 * auditService.record(userId, 'ATTENDANCE_MODIFICATION_REQUEST_INFO',
                'AttendanceModificationRequest', requestId.toString(), tontineId, _)
    }

    @Unroll
    def "decide : refuse depuis l'etat final #status"() {
        given:
        def r = request(AttendanceStatus.ABSENT, AttendanceStatus.PRESENT, status)
        repository.findById(requestId) >> Optional.of(r)

        when:
        service.decide(requestId, tontineId, userId,
                new DecideAttendanceModificationRequest(
                        AttendanceModificationDecision.APPROVE, 'x', null))

        then:
        ApiException ex = thrown()
        ex.code == 'ATTENDANCE_MODIFICATION_INVALID_STATE'

        where:
        status << [AttendanceModificationStatus.APPROVED, AttendanceModificationStatus.REJECTED]
    }

    def "decide : refuse une demande d'une autre tontine"() {
        given:
        def r = request(AttendanceStatus.ABSENT, AttendanceStatus.PRESENT).tap {
            it.tontineId = UUID.randomUUID()
        }
        repository.findById(requestId) >> Optional.of(r)

        when:
        service.decide(requestId, tontineId, userId,
                new DecideAttendanceModificationRequest(
                        AttendanceModificationDecision.APPROVE, 'x', null))

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
    }
}
