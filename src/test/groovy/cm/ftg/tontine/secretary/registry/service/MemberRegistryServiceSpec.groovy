package cm.ftg.tontine.secretary.registry.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.member.entity.Member
import cm.ftg.tontine.member.repository.MemberRepository
import cm.ftg.tontine.secretary.registry.dto.UpdateMemberRegistryRequest
import cm.ftg.tontine.secretary.security.SecretaryAccessChecker
import spock.lang.Specification
import spock.lang.Subject

class MemberRegistryServiceSpec extends Specification {

    MemberRepository memberRepository = Mock()
    SecretaryAccessChecker accessChecker = Mock()
    AuditService auditService = Mock()

    @Subject
    MemberRegistryService service = new MemberRegistryService(
            memberRepository, accessChecker, auditService)

    UUID tontineId = UUID.randomUUID()
    UUID userId = UUID.randomUUID()

    Member existingMember() {
        new Member().tap {
            id = UUID.randomUUID()
            it.tontineId = this.tontineId
            firstName = 'Jean'
            lastName = 'Mbarga'
            phone = '+237699000111'
            email = 'jm@example.com'
            matricule = 'M-001'
        }
    }

    def "update : modifie uniquement les champs fournis et logge les changements"() {
        given:
        def m = existingMember()
        memberRepository.findById(m.id) >> Optional.of(m)
        memberRepository.save(_) >> { it[0] }
        def req = new UpdateMemberRegistryRequest('+237600999888', null, 'M-042')

        when:
        def dto = service.update(m.id, tontineId, userId, req)

        then:
        m.phone == '+237600999888'
        m.email == 'jm@example.com'      // inchange
        m.matricule == 'M-042'
        dto.phone == '+237600999888'
        1 * auditService.record(userId, 'MEMBER_REGISTRY_UPDATE', 'Member', m.id.toString(),
                tontineId, { it.contains('phone') && it.contains('matricule') && !it.contains('email') })
    }

    def "update : sans changement reel ne touche pas les champs"() {
        given:
        def m = existingMember()
        memberRepository.findById(m.id) >> Optional.of(m)
        memberRepository.save(_) >> { it[0] }
        def req = new UpdateMemberRegistryRequest('+237699000111', 'jm@example.com', 'M-001')

        when:
        service.update(m.id, tontineId, userId, req)

        then:
        1 * auditService.record(userId, 'MEMBER_REGISTRY_UPDATE', _, _, _,
                { it == '{}' })   // aucun champ touche
    }

    def "update : refuse un membre hors tontine"() {
        given:
        def m = existingMember().tap { it.tontineId = UUID.randomUUID() }
        memberRepository.findById(m.id) >> Optional.of(m)

        when:
        service.update(m.id, tontineId, userId,
                new UpdateMemberRegistryRequest('+1', null, null))

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
        0 * memberRepository.save(_)
    }

    def "update : 404 quand le membre n'existe pas"() {
        given:
        UUID missing = UUID.randomUUID()
        memberRepository.findById(missing) >> Optional.empty()

        when:
        service.update(missing, tontineId, userId,
                new UpdateMemberRegistryRequest(null, null, null))

        then:
        thrown(cm.ftg.tontine.common.exception.ResourceNotFoundException)
    }
}
