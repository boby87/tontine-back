package cm.ftg.tontine.auditor.certification.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.auditor.certification.dto.IssueCertificationRequest
import cm.ftg.tontine.auditor.certification.entity.Certification
import cm.ftg.tontine.auditor.certification.enums.CertificationDecision
import cm.ftg.tontine.auditor.certification.enums.CertificationScope
import cm.ftg.tontine.auditor.certification.repository.CertificationRepository
import cm.ftg.tontine.auditor.security.AuditorAccessChecker
import cm.ftg.tontine.member.entity.Member
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class CertificationServiceSpec extends Specification {

    CertificationRepository repository = Mock()
    AuditorAccessChecker accessChecker = Mock()
    AuditService auditService = Mock()

    @Subject
    CertificationService service = new CertificationService(
            repository, accessChecker, auditService)

    UUID tontineId = UUID.randomUUID()
    UUID userId = UUID.randomUUID()
    UUID certificationId = UUID.randomUUID()

    Member auditor = new Member().tap { firstName = 'Audrey'; lastName = 'Tor' }

    def setup() {
        accessChecker.requireAuditor(userId, tontineId) >> auditor
    }

    @Unroll
    def "issue #decision : persiste avec nom de l'auditeur et audit"() {
        given:
        repository.save(_) >> { Certification c -> c.id = certificationId; c }
        def req = new IssueCertificationRequest(
                CertificationScope.MONTH, '2026-05', decision, reserves, '123456')

        when:
        def dto = service.issue(tontineId, userId, req)

        then:
        dto.decision == decision
        dto.scope == CertificationScope.MONTH
        dto.periodLabel == '2026-05'
        dto.reserves == reserves
        dto.issuedByFullName == 'Audrey Tor'
        1 * auditService.record(userId, 'CERTIFICATION_ISSUE', 'Certification',
                certificationId.toString(), tontineId,
                { it.contains(decision.name()) && it.contains('MONTH') })

        where:
        decision                                       | reserves
        CertificationDecision.CERTIFIED                | null
        CertificationDecision.CERTIFIED_WITH_RESERVES  | '2 reserves mineures'
        CertificationDecision.REFUSED                  | 'ecarts non resolus'
    }

    @Unroll
    def "issue : portee #scope est conservee"() {
        given:
        repository.save(_) >> { Certification c -> c.id = certificationId; c }

        when:
        def dto = service.issue(tontineId, userId, new IssueCertificationRequest(
                scope, label, CertificationDecision.CERTIFIED, null, '123456'))

        then:
        dto.scope == scope
        dto.periodLabel == label

        where:
        scope                       | label
        CertificationScope.MONTH    | '2026-05'
        CertificationScope.CYCLE    | 'Cycle #3'
        CertificationScope.YEAR     | '2026'
    }
}
