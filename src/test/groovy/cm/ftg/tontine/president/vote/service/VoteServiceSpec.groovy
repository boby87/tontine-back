package cm.ftg.tontine.president.vote.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.auth.entity.UserEntity
import cm.ftg.tontine.auth.repository.UserRepository
import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.president.security.PresidentAccessChecker
import cm.ftg.tontine.president.vote.dto.CreateVoteRequest
import cm.ftg.tontine.president.vote.entity.Vote
import cm.ftg.tontine.president.vote.entity.VoteOption
import cm.ftg.tontine.president.vote.enums.VoteAudience
import cm.ftg.tontine.president.vote.enums.VoteScope
import cm.ftg.tontine.president.vote.enums.VoteStatus
import cm.ftg.tontine.president.vote.repository.VoteOptionRepository
import cm.ftg.tontine.president.vote.repository.VoteRepository
import cm.ftg.tontine.realtime.RealtimeEventPublisher
import spock.lang.Specification
import spock.lang.Subject

import java.time.Instant

class VoteServiceSpec extends Specification {

    VoteRepository voteRepository = Mock()
    VoteOptionRepository optionRepository = Mock()
    PresidentAccessChecker accessChecker = Mock()
    AuditService auditService = Mock()
    UserRepository userRepository = Mock()
    RealtimeEventPublisher realtime = Mock()

    @Subject
    VoteService service = new VoteService(
            voteRepository, optionRepository, accessChecker, auditService, userRepository, realtime)

    UUID tontineId = UUID.randomUUID()
    UUID userId = UUID.randomUUID()
    UUID voteId = UUID.randomUUID()

    def setup() {
        userRepository.findById(userId) >> Optional.of(new UserEntity().tap {
            firstName = 'P'; lastName = 'Resident'; email = 'p@r.com'
        })
    }

    CreateVoteRequest req(Instant opensAt, Instant closesAt, List<String> options = ['Oui', 'Non']) {
        new CreateVoteRequest('Faut-il proceder ?', null, options, false, false,
                VoteScope.STANDARD, VoteAudience.ALL, opensAt, closesAt, 50.00)
    }

    def "create : refuse moins de 2 options"() {
        when:
        service.create(tontineId, userId, req(
                Instant.now(), Instant.now().plusSeconds(3600), ['Oui']))

        then:
        ApiException ex = thrown()
        ex.code == 'VOTE_INVALID_OPTIONS'
        ex.status.value() == 422
    }

    def "create : refuse opensAt >= closesAt"() {
        given:
        def t = Instant.now()

        when:
        service.create(tontineId, userId, req(t.plusSeconds(3600), t))

        then:
        ApiException ex = thrown()
        ex.code == 'VOTE_INVALID_WINDOW'
    }

    def "create : status OPEN quand opensAt est deja passe"() {
        given:
        voteRepository.save(_) >> { Vote v -> v.id = voteId; v }
        optionRepository.saveAll(_) >> { call ->
            def opts = call[0] as List
            opts.each { o -> o.id = UUID.randomUUID() }
            opts
        }

        when:
        def dto = service.create(tontineId, userId, req(
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600)))

        then:
        dto.status == VoteStatus.OPEN
        dto.options.size() == 2
        dto.createdByFullName == 'P Resident'
        1 * realtime.toVote(tontineId, voteId, 'vote.created', _)
        1 * auditService.record(userId, 'VOTE_CREATE', 'Vote', voteId.toString(), tontineId, _)
    }

    def "create : status DRAFT quand opensAt est dans le futur"() {
        given:
        voteRepository.save(_) >> { Vote v -> v.id = voteId; v }
        optionRepository.saveAll(_) >> { call -> call[0] }

        when:
        def dto = service.create(tontineId, userId, req(
                Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200)))

        then:
        dto.status == VoteStatus.DRAFT
    }

    def "close : refuse un vote non OPEN"() {
        given:
        def v = new Vote().tap {
            id = voteId; it.tontineId = this.tontineId; status = VoteStatus.DRAFT
        }
        voteRepository.findById(voteId) >> Optional.of(v)

        when:
        service.close(voteId, tontineId, userId)

        then:
        ApiException ex = thrown()
        ex.code == 'VOTE_INVALID_STATE'
    }

    def "close : passed=true quand la premiere option est strictement majoritaire"() {
        given:
        def v = new Vote().tap {
            id = voteId; it.tontineId = this.tontineId; status = VoteStatus.OPEN
        }
        voteRepository.findById(voteId) >> Optional.of(v)
        voteRepository.save(_) >> { it[0] }
        optionRepository.findByVoteIdOrderByDisplayOrderAsc(voteId) >> [
                new VoteOption().tap { label = 'Oui'; count = 10; displayOrder = 0 },
                new VoteOption().tap { label = 'Non'; count = 4; displayOrder = 1 }
        ]

        when:
        def dto = service.close(voteId, tontineId, userId)

        then:
        dto.status == VoteStatus.CLOSED
        v.passed
        1 * realtime.toVote(tontineId, voteId, 'vote.closed', _)
    }

    def "close : passed=false en cas d'egalite avec la premiere option"() {
        given:
        def v = new Vote().tap {
            id = voteId; it.tontineId = this.tontineId; status = VoteStatus.OPEN
        }
        voteRepository.findById(voteId) >> Optional.of(v)
        voteRepository.save(_) >> { it[0] }
        optionRepository.findByVoteIdOrderByDisplayOrderAsc(voteId) >> [
                new VoteOption().tap { label = 'A'; count = 5; displayOrder = 0 },
                new VoteOption().tap { label = 'B'; count = 5; displayOrder = 1 }
        ]

        when:
        def dto = service.close(voteId, tontineId, userId)

        then:
        !v.passed
        dto.status == VoteStatus.CLOSED
    }

    def "close : refuse un vote d'une autre tontine"() {
        given:
        def v = new Vote().tap {
            id = voteId; it.tontineId = UUID.randomUUID(); status = VoteStatus.OPEN
        }
        voteRepository.findById(voteId) >> Optional.of(v)

        when:
        service.close(voteId, tontineId, userId)

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
    }
}
