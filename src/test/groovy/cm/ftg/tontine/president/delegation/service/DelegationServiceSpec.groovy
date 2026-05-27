package cm.ftg.tontine.president.delegation.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.auth.entity.UserEntity
import cm.ftg.tontine.auth.repository.UserRepository
import cm.ftg.tontine.common.enums.UserRole
import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.president.delegation.dto.CreateDelegationRequest
import cm.ftg.tontine.president.delegation.dto.RevokeDelegationRequest
import cm.ftg.tontine.president.delegation.entity.Delegation
import cm.ftg.tontine.president.delegation.enums.DelegationPower
import cm.ftg.tontine.president.delegation.enums.DelegationStatus
import cm.ftg.tontine.president.delegation.repository.DelegationRepository
import cm.ftg.tontine.president.security.PresidentAccessChecker
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.time.Instant

class DelegationServiceSpec extends Specification {

    DelegationRepository repository = Mock()
    PresidentAccessChecker accessChecker = Mock()
    AuditService auditService = Mock()
    UserRepository userRepository = Mock()

    @Subject
    DelegationService service = new DelegationService(
            repository, accessChecker, auditService, userRepository)

    UUID tontineId = UUID.randomUUID()
    UUID userId = UUID.randomUUID()
    UUID delegateeId = UUID.randomUUID()
    UUID delegationId = UUID.randomUUID()

    UserEntity delegatee(UserRole... roles) {
        new UserEntity().tap {
            id = delegateeId
            firstName = 'Marie'
            lastName = 'Curie'
            email = 'mc@example.com'
            it.roles = roles.length == 0 ? EnumSet.noneOf(UserRole) : EnumSet.copyOf(roles.toList())
        }
    }

    def "create : delegation active, role principal selectionne par priorite"() {
        given:
        userRepository.findById(delegateeId) >> Optional.of(delegatee(UserRole.TREASURER, UserRole.SECRETARY))
        repository.save(_) >> { Delegation d -> d.id = delegationId; d }
        def opensAt = Instant.now()
        def closesAt = opensAt.plusSeconds(7 * 86400)
        def req = new CreateDelegationRequest(
                delegateeId,
                [DelegationPower.VALIDATE_DOCUMENTS, DelegationPower.PRESIDE_SESSION] as Set,
                'Absence du president',
                opensAt, closesAt)

        when:
        def dto = service.create(tontineId, userId, req)

        then:
        dto.status == DelegationStatus.ACTIVE
        dto.delegateeRole == UserRole.SECRETARY   // SECRETARY a priorite sur TREASURER dans la liste
        dto.delegateeFullName == 'Marie Curie'
        dto.powers.size() == 2
        1 * auditService.record(userId, 'DELEGATION_CREATE', 'Delegation', delegationId.toString(),
                tontineId, { it.contains(delegateeId.toString()) })
    }

    def "create : utilise MEMBER si aucun role"() {
        given:
        userRepository.findById(delegateeId) >> Optional.of(delegatee())
        repository.save(_) >> { Delegation d -> d.id = delegationId; d }

        when:
        def dto = service.create(tontineId, userId, new CreateDelegationRequest(
                delegateeId, [DelegationPower.VALIDATE_DOCUMENTS] as Set,
                'r', Instant.now(), Instant.now().plusSeconds(60)))

        then:
        dto.delegateeRole == UserRole.MEMBER
    }

    def "create : refuse startsAt >= endsAt"() {
        given:
        def t = Instant.now()

        when:
        service.create(tontineId, userId, new CreateDelegationRequest(
                delegateeId, [DelegationPower.VALIDATE_DOCUMENTS] as Set,
                'r', t.plusSeconds(60), t))

        then:
        ApiException ex = thrown()
        ex.code == 'DELEGATION_INVALID_WINDOW'
        ex.status.value() == 422
    }

    def "create : 404 quand le delegataire n'existe pas"() {
        given:
        userRepository.findById(delegateeId) >> Optional.empty()

        when:
        service.create(tontineId, userId, new CreateDelegationRequest(
                delegateeId, [DelegationPower.VALIDATE_DOCUMENTS] as Set,
                'r', Instant.now(), Instant.now().plusSeconds(60)))

        then:
        thrown(cm.ftg.tontine.common.exception.ResourceNotFoundException)
    }

    def "revoke : passe ACTIVE -> REVOKED avec timestamp et motif"() {
        given:
        def d = new Delegation().tap {
            id = delegationId
            it.tontineId = this.tontineId
            status = DelegationStatus.ACTIVE
        }
        repository.findById(delegationId) >> Optional.of(d)
        repository.save(_) >> { it[0] }

        when:
        def dto = service.revoke(delegationId, tontineId, userId,
                new RevokeDelegationRequest('Erreur de delegation'))

        then:
        dto.status == DelegationStatus.REVOKED
        d.revokedAt != null
        d.revokedReason == 'Erreur de delegation'
        1 * auditService.record(userId, 'DELEGATION_REVOKE', 'Delegation', delegationId.toString(),
                tontineId, { it.contains('Erreur de delegation') })
    }

    @Unroll
    def "revoke : refuse une delegation non ACTIVE (#status)"() {
        given:
        def d = new Delegation().tap {
            id = delegationId; it.tontineId = this.tontineId; it.status = status
        }
        repository.findById(delegationId) >> Optional.of(d)

        when:
        service.revoke(delegationId, tontineId, userId, new RevokeDelegationRequest('x'))

        then:
        ApiException ex = thrown()
        ex.code == 'DELEGATION_INVALID_STATE'
        ex.status.value() == 409

        where:
        status << [DelegationStatus.REVOKED, DelegationStatus.EXPIRED]
    }

    def "revoke : refuse une delegation d'une autre tontine"() {
        given:
        def d = new Delegation().tap {
            id = delegationId; it.tontineId = UUID.randomUUID(); status = DelegationStatus.ACTIVE
        }
        repository.findById(delegationId) >> Optional.of(d)

        when:
        service.revoke(delegationId, tontineId, userId, new RevokeDelegationRequest('x'))

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
    }
}
