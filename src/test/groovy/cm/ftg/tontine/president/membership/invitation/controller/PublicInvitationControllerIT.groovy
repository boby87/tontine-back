package cm.ftg.tontine.president.membership.invitation.controller

import cm.ftg.tontine.auth.repository.UserRepository
import cm.ftg.tontine.common.enums.ContributionFrequency
import cm.ftg.tontine.common.enums.MemberStatus
import cm.ftg.tontine.common.enums.TontineStatus
import cm.ftg.tontine.common.enums.UserRole
import cm.ftg.tontine.member.entity.Member
import cm.ftg.tontine.member.repository.MemberRepository
import cm.ftg.tontine.president.membership.invitation.entity.MembershipInvitation
import cm.ftg.tontine.president.membership.invitation.enums.InvitationStatus
import cm.ftg.tontine.president.membership.invitation.repository.MembershipInvitationRepository
import cm.ftg.tontine.tontine.dto.TontineRulesDto
import cm.ftg.tontine.tontine.entity.Tontine
import cm.ftg.tontine.tontine.repository.TontineRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class PublicInvitationControllerIT extends Specification {

    @Autowired
    MockMvc mockMvc
    @Autowired
    MembershipInvitationRepository invitationRepository
    @Autowired
    MemberRepository memberRepository
    @Autowired
    UserRepository userRepository
    @Autowired
    TontineRepository tontineRepository

    UUID tontineId
    UUID inviterId = UUID.randomUUID()

    def setup() {
        invitationRepository.deleteAll()
        memberRepository.deleteAll()
        userRepository.deleteAll()
        tontineRepository.deleteAll()

        tontine(1)
    }

    Tontine tontine(int memberCount) {
        def t = tontineRepository.save(new Tontine().tap {
            name = 'Tontine Publique'
            status = TontineStatus.ACTIVE
            contributionAmount = BigDecimal.valueOf(5000)
            frequency = ContributionFrequency.MONTHLY
            startDate = LocalDate.now()
            maxMembers = 10
            createdByUserId = this.inviterId
            totalSaved = BigDecimal.ZERO
            it.memberCount = memberCount
            rules = new TontineRulesDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.valueOf(100000), BigDecimal.valueOf(5), 12,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO).toEmbeddable()
        })
        tontineId = t.id
        t
    }

    MembershipInvitation invitation(Map args = [:]) {
        invitationRepository.save(new MembershipInvitation().tap {
            it.tontineId = this.tontineId
            candidateFullName = args.name ?: 'Jean Tagne'
            candidatePhone = args.phone ?: '+237699000099'
            candidateEmail = args.containsKey('email') ? args.email : 'jean@example.cm'
            proposedRole = (args.role ?: UserRole.MEMBER) as UserRole
            channels = args.channels ?: 'SMS,EMAIL'
            status = (args.status ?: InvitationStatus.SENT) as InvitationStatus
            token = args.token
            expiresAt = args.expiresAt ?: Instant.now().plus(7, ChronoUnit.DAYS)
            invitedByUserId = this.inviterId
            invitedByFullName = 'Pres Ident'
        })
    }

    // ---------- 12 ----------

    def "12 - preview : token valide -> 200 expired=false alreadyAccepted=false"() {
        given:
        invitation(token: 'tok-valid-12')

        expect:
        mockMvc.perform(get('/auth/invitations/tok-valid-12/preview'))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data.tontineName').value('Tontine Publique'))
                .andExpect(jsonPath('$.data.proposedRole').value('MEMBER'))
                .andExpect(jsonPath('$.data.expired').value(false))
                .andExpect(jsonPath('$.data.alreadyAccepted').value(false))
    }

    // ---------- 13 ----------

    def "13 - preview : token expire -> 200 expired=true"() {
        given:
        invitation(token: 'tok-expired-13', expiresAt: Instant.now().minus(1, ChronoUnit.DAYS))

        expect:
        mockMvc.perform(get('/auth/invitations/tok-expired-13/preview'))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data.expired').value(true))
    }

    // ---------- 14 ----------

    def "14 - preview : token inexistant -> 404"() {
        expect:
        mockMvc.perform(get('/auth/invitations/does-not-exist/preview'))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath('$.code').value('INVITATION_NOT_FOUND'))
    }

    // ---------- 15 ----------

    def "15 - accept : token valide -> 200, session + user + member ACTIVE"() {
        given:
        invitation(token: 'tok-accept-15', phone: '+237699000015', email: 'cand15@example.cm')

        when:
        def result = mockMvc.perform(post('/auth/invitations/tok-accept-15/accept')
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"password":"Welcome123!"}'))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.data.session.tokens.accessToken').exists())
                .andExpect(jsonPath('$.data.memberId').exists())
                .andExpect(jsonPath('$.data.tontineName').value('Tontine Publique'))

        and:
        def user = userRepository.findByPhone('+237699000015').orElse(null)
        user != null
        def member = memberRepository.findByUserIdAndTontineId(user.id, tontineId).orElse(null)
        member != null
        member.status == MemberStatus.ACTIVE
        member.roles.contains(UserRole.MEMBER)

        and:
        invitationRepository.findByToken('tok-accept-15').get().status == InvitationStatus.ACCEPTED
    }

    // ---------- 16 ----------

    def "16 - accept : token expire -> 422 INVITATION_EXPIRED"() {
        given:
        invitation(token: 'tok-accept-16', phone: '+237699000016',
                expiresAt: Instant.now().minus(1, ChronoUnit.DAYS))

        expect:
        mockMvc.perform(post('/auth/invitations/tok-accept-16/accept')
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"password":"Welcome123!"}'))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath('$.code').value('INVITATION_EXPIRED'))
    }

    // ---------- 17 ----------

    def "17 - accept : deja accepte -> 409 INVITATION_ALREADY_ACCEPTED"() {
        given:
        invitation(token: 'tok-accept-17', phone: '+237699000017', status: InvitationStatus.ACCEPTED)

        expect:
        mockMvc.perform(post('/auth/invitations/tok-accept-17/accept')
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"password":"Welcome123!"}'))
                .andExpect(status().isConflict())
                .andExpect(jsonPath('$.code').value('INVITATION_ALREADY_ACCEPTED'))
    }

    // ---------- 18 ----------

    def "18 - accept : annule -> 422 INVITATION_CANCELLED"() {
        given:
        invitation(token: 'tok-accept-18', phone: '+237699000018', status: InvitationStatus.CANCELLED)

        expect:
        mockMvc.perform(post('/auth/invitations/tok-accept-18/accept')
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"password":"Welcome123!"}'))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath('$.code').value('INVITATION_CANCELLED'))
    }

    // ---------- 19 ----------

    def "19 - accept : tontine.memberCount incremente"() {
        given: 'un membre existant (memberCount=1) puis acceptation'
        memberRepository.save(new Member().tap {
            it.tontineId = this.tontineId
            matricule = 'M-001'
            firstName = 'Pres'; lastName = 'Ident'; phone = '+237699000001'
            it.roles = [UserRole.PRESIDENT, UserRole.MEMBER] as Set
            it.status = MemberStatus.ACTIVE
        })
        invitation(token: 'tok-accept-19', phone: '+237699000019', email: 'cand19@example.cm')

        when:
        mockMvc.perform(post('/auth/invitations/tok-accept-19/accept')
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"password":"Welcome123!"}'))
                .andExpect(status().isOk())

        then:
        tontineRepository.findById(tontineId).get().memberCount == 2
    }
}
