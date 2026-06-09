package cm.ftg.tontine.member.vote.controller

import cm.ftg.tontine.auth.entity.UserEntity
import cm.ftg.tontine.auth.repository.UserRepository
import cm.ftg.tontine.common.enums.MemberStatus
import cm.ftg.tontine.common.enums.UserRole
import cm.ftg.tontine.member.entity.Member
import cm.ftg.tontine.member.repository.MemberRepository
import cm.ftg.tontine.member.vote.repository.VoteBallotRepository
import cm.ftg.tontine.president.vote.entity.Vote
import cm.ftg.tontine.president.vote.entity.VoteOption
import cm.ftg.tontine.president.vote.enums.VoteAudience
import cm.ftg.tontine.president.vote.enums.VoteScope
import cm.ftg.tontine.president.vote.enums.VoteStatus
import cm.ftg.tontine.president.vote.repository.VoteOptionRepository
import cm.ftg.tontine.president.vote.repository.VoteRepository
import cm.ftg.tontine.security.AuthenticatedUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import java.math.BigDecimal
import java.time.Instant

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class MemberVoteControllerIT extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    VoteRepository voteRepository
    @Autowired
    VoteOptionRepository optionRepository
    @Autowired
    VoteBallotRepository ballotRepository
    @Autowired
    MemberRepository memberRepository
    @Autowired
    UserRepository userRepository

    UUID tontineId = UUID.randomUUID()
    UUID userId
    AuthenticatedUser principal

    def setup() {
        ballotRepository.deleteAll()
        optionRepository.deleteAll()
        voteRepository.deleteAll()
        memberRepository.deleteAll()
        userRepository.deleteAll()

        def suffix = UUID.randomUUID().toString().replace('-', '').substring(0, 10)
        def user = userRepository.save(new UserEntity().tap {
            firstName = 'Jean'; lastName = 'Membre'
            email = "membre_${suffix}@example.com"
            phone = "+23769${suffix.substring(0, 7)}"
            passwordHash = 'hash'
            roles = [UserRole.MEMBER] as Set
            active = true
        })
        userId = user.id
        principal = new AuthenticatedUser(userId, user.email, user.phone, 'hash',
                [UserRole.MEMBER] as Set, true)
        SecurityContextHolder.setContext(new SecurityContextImpl(
                new UsernamePasswordAuthenticationToken(principal, null, principal.authorities)))
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    // ---------- helpers ----------

    Member member(Set<UserRole> roles, MemberStatus status) {
        memberRepository.save(new Member().tap {
            it.userId = this.userId
            it.tontineId = this.tontineId
            matricule = 'M-' + UUID.randomUUID().toString().substring(0, 8)
            firstName = 'Jean'; lastName = 'Membre'; phone = '+237600000000'
            it.roles = roles
            it.status = status
        })
    }

    Vote vote(Map args = [:]) {
        voteRepository.save(new Vote().tap {
            it.tontineId = this.tontineId
            question = args.question ?: 'Faut-il proceder ?'
            anonymous = args.anonymous ?: false
            hideResultsUntilClose = args.hide ?: false
            scope = (args.scope ?: VoteScope.STANDARD) as VoteScope
            audience = (args.audience ?: VoteAudience.ALL) as VoteAudience
            status = (args.status ?: VoteStatus.OPEN) as VoteStatus
            opensAt = args.opensAt ?: Instant.now().minusSeconds(60)
            closesAt = args.closesAt ?: Instant.now().plusSeconds(3600)
            createdByUserId = this.userId
            createdByFullName = 'Jean Membre'
            totalVoters = (args.totalVoters ?: 0) as int
            totalVoted = (args.totalVoted ?: 0) as int
            quorumPercent = (args.quorum != null ? args.quorum : BigDecimal.ZERO) as BigDecimal
        })
    }

    VoteOption option(UUID voteId, String label, int order, long count = 0) {
        optionRepository.save(new VoteOption().tap {
            it.voteId = voteId; it.label = label; displayOrder = order; it.count = count
        })
    }

    String castBody(UUID optionId) {
        "{\"optionId\":\"${optionId}\"}"
    }

    def headers(builder) {
        builder.header('X-Tontine-Id', tontineId.toString())
    }

    // ---------- 1 ----------

    def "1 - liste vide quand aucun vote"() {
        given:
        member([UserRole.MEMBER] as Set, MemberStatus.ACTIVE)

        expect:
        mockMvc.perform(headers(get('/members/me/votes')))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data').isArray())
                .andExpect(jsonPath('$.data').isEmpty())
    }

    // ---------- 2 ----------

    def "2 - un vote BUREAU n'apparait pas pour un MEMBER simple"() {
        given:
        member([UserRole.MEMBER] as Set, MemberStatus.ACTIVE)
        def v = vote(audience: VoteAudience.BUREAU, status: VoteStatus.OPEN)
        option(v.id, 'Oui', 0)
        option(v.id, 'Non', 1)

        expect:
        mockMvc.perform(headers(get('/members/me/votes')))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data').isArray())
                .andExpect(jsonPath('$.data.length()').value(0))
    }

    // ---------- 3 ----------

    def "3 - cast succes : insere le ballot et incremente les compteurs"() {
        given:
        member([UserRole.MEMBER] as Set, MemberStatus.ACTIVE)
        def v = vote(audience: VoteAudience.ALL, status: VoteStatus.OPEN)
        def alice = option(v.id, 'Alice', 0)
        option(v.id, 'Bob', 1)

        when:
        def result = mockMvc.perform(headers(post("/members/me/votes/${v.id}/cast"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(castBody(alice.id)))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.data.optionLabel').value('Alice'))
                .andExpect(jsonPath('$.data.voteId').value(v.id.toString()))
                .andExpect(jsonPath('$.message').value('Vote enregistre'))

        and:
        ballotRepository.count() == 1
        optionRepository.findById(alice.id).get().count == 1L
        voteRepository.findById(v.id).get().totalVoted == 1
    }

    // ---------- 4 ----------

    def "4 - cast deux fois : le second est refuse en 409"() {
        given:
        member([UserRole.MEMBER] as Set, MemberStatus.ACTIVE)
        def v = vote(audience: VoteAudience.ALL, status: VoteStatus.OPEN)
        def opt = option(v.id, 'Oui', 0)
        option(v.id, 'Non', 1)

        and:
        mockMvc.perform(headers(post("/members/me/votes/${v.id}/cast"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(castBody(opt.id)))
                .andExpect(status().isOk())

        expect:
        mockMvc.perform(headers(post("/members/me/votes/${v.id}/cast"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(castBody(opt.id)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath('$.code').value('VOTE_ALREADY_CAST'))
    }

    // ---------- 5 ----------

    def "5 - cast sur un vote CLOSED : 422 VOTE_NOT_OPEN"() {
        given:
        member([UserRole.MEMBER] as Set, MemberStatus.ACTIVE)
        def v = vote(audience: VoteAudience.ALL, status: VoteStatus.CLOSED)
        def opt = option(v.id, 'Oui', 0)
        option(v.id, 'Non', 1)

        expect:
        mockMvc.perform(headers(post("/members/me/votes/${v.id}/cast"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(castBody(opt.id)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath('$.code').value('VOTE_NOT_OPEN'))
    }

    // ---------- 6 ----------

    def "6 - cast avant opensAt : 422 VOTE_OUTSIDE_WINDOW"() {
        given:
        member([UserRole.MEMBER] as Set, MemberStatus.ACTIVE)
        def v = vote(audience: VoteAudience.ALL, status: VoteStatus.OPEN,
                opensAt: Instant.now().plusSeconds(3600),
                closesAt: Instant.now().plusSeconds(7200))
        def opt = option(v.id, 'Oui', 0)
        option(v.id, 'Non', 1)

        expect:
        mockMvc.perform(headers(post("/members/me/votes/${v.id}/cast"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(castBody(opt.id)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath('$.code').value('VOTE_OUTSIDE_WINDOW'))
    }

    // ---------- 7 ----------

    def "7 - cast avec un optionId appartenant a un autre vote : 422 VOTE_OPTION_MISMATCH"() {
        given:
        member([UserRole.MEMBER] as Set, MemberStatus.ACTIVE)
        def v1 = vote(audience: VoteAudience.ALL, status: VoteStatus.OPEN)
        option(v1.id, 'A1', 0)
        def v2 = vote(audience: VoteAudience.ALL, status: VoteStatus.OPEN)
        def foreignOption = option(v2.id, 'B1', 0)

        expect:
        mockMvc.perform(headers(post("/members/me/votes/${v1.id}/cast"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(castBody(foreignOption.id)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath('$.code').value('VOTE_OPTION_MISMATCH'))
    }

    // ---------- 8 ----------

    def "8 - cast non-eligible (mauvaise audience) : 403 VOTE_NOT_ELIGIBLE"() {
        given:
        member([UserRole.MEMBER] as Set, MemberStatus.ACTIVE)
        def v = vote(audience: VoteAudience.BUREAU, status: VoteStatus.OPEN)
        def opt = option(v.id, 'Oui', 0)
        option(v.id, 'Non', 1)

        expect:
        mockMvc.perform(headers(post("/members/me/votes/${v.id}/cast"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(castBody(opt.id)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath('$.code').value('VOTE_NOT_ELIGIBLE'))
    }

    // ---------- 9 ----------

    def "9 - vote anonyme : member_id NULL et voter_key est un hash"() {
        given:
        member([UserRole.MEMBER] as Set, MemberStatus.ACTIVE)
        def v = vote(audience: VoteAudience.ALL, status: VoteStatus.OPEN, anonymous: true)
        def opt = option(v.id, 'Oui', 0)
        option(v.id, 'Non', 1)

        when:
        mockMvc.perform(headers(post("/members/me/votes/${v.id}/cast"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(castBody(opt.id)))
                .andExpect(status().isOk())

        then:
        def ballot = ballotRepository.findAll().first()
        ballot.memberId == null
        ballot.voterKey != null
        ballot.voterKey.length() == 64
        ballot.voterKey ==~ /[0-9a-f]{64}/
    }

    // ---------- 10 ----------

    def "10 - hideResultsUntilClose=true + OPEN : compteurs masques (0) cote GET"() {
        given:
        member([UserRole.MEMBER] as Set, MemberStatus.ACTIVE)
        def v = vote(audience: VoteAudience.ALL, status: VoteStatus.OPEN, hide: true, totalVoted: 8)
        option(v.id, 'Oui', 0, 3L)
        option(v.id, 'Non', 1, 5L)

        expect:
        mockMvc.perform(headers(get("/members/me/votes/${v.id}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data.totalVoted').value(0))
                .andExpect(jsonPath('$.data.options[0].count').value(0))
                .andExpect(jsonPath('$.data.options[1].count').value(0))
    }

    // ---------- 11 ----------

    def "11 - hideResultsUntilClose=true + CLOSED : vrais compteurs"() {
        given:
        member([UserRole.MEMBER] as Set, MemberStatus.ACTIVE)
        def v = vote(audience: VoteAudience.ALL, status: VoteStatus.CLOSED, hide: true, totalVoted: 8)
        option(v.id, 'Oui', 0, 3L)
        option(v.id, 'Non', 1, 5L)

        expect:
        mockMvc.perform(headers(get("/members/me/votes/${v.id}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data.totalVoted').value(8))
                .andExpect(jsonPath('$.data.options[0].count').value(3))
                .andExpect(jsonPath('$.data.options[1].count').value(5))
    }

    // ---------- 12 ----------

    def "12 - quorum non atteint a la cloture : passed=false"() {
        given: 'un membre President pour pouvoir cloturer'
        member([UserRole.PRESIDENT] as Set, MemberStatus.ACTIVE)
        def v = vote(audience: VoteAudience.ALL, status: VoteStatus.OPEN,
                totalVoters: 10, totalVoted: 2, quorum: new BigDecimal('50.00'))
        option(v.id, 'Oui', 0, 2L)
        option(v.id, 'Non', 1, 0L)

        when:
        mockMvc.perform(headers(post("/president/votes/${v.id}/close")))
                .andExpect(status().isOk())

        then:
        def closed = voteRepository.findById(v.id).get()
        closed.status == VoteStatus.CLOSED
        closed.passed == Boolean.FALSE
    }
}
