package cm.ftg.tontine.president.membership.invitation.controller

import cm.ftg.tontine.auth.entity.UserEntity
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
import cm.ftg.tontine.security.AuthenticatedUser
import cm.ftg.tontine.tontine.dto.TontineRulesDto
import cm.ftg.tontine.tontine.entity.Tontine
import cm.ftg.tontine.tontine.repository.TontineRepository
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
import java.time.LocalDate
import java.time.temporal.ChronoUnit

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class PresidentInvitationControllerIT extends Specification {

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
    UUID userId
    AuthenticatedUser principal
    Member presidentMember
    Tontine tontine

    def setup() {
        invitationRepository.deleteAll()
        memberRepository.deleteAll()
        userRepository.deleteAll()
        tontineRepository.deleteAll()

        def suffix = UUID.randomUUID().toString().replace('-', '').substring(0, 10)
        def user = userRepository.save(new UserEntity().tap {
            firstName = 'Pres'; lastName = 'Ident'
            email = "pres_${suffix}@example.com"
            phone = "+23769${suffix.substring(0, 7)}"
            passwordHash = 'hash'
            roles = [UserRole.PRESIDENT, UserRole.MEMBER] as Set
            active = true
        })
        userId = user.id

        tontine = tontineRepository.save(new Tontine().tap {
            name = 'Tontine Test'
            status = TontineStatus.ACTIVE
            contributionAmount = BigDecimal.valueOf(10000)
            frequency = ContributionFrequency.MONTHLY
            startDate = LocalDate.now()
            maxMembers = 10
            createdByUserId = this.userId
            totalSaved = BigDecimal.ZERO
            memberCount = 1
            rules = new TontineRulesDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.valueOf(100000), BigDecimal.valueOf(5), 12,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO).toEmbeddable()
        })
        tontineId = tontine.id

        presidentMember = memberRepository.save(new Member().tap {
            it.userId = this.userId
            it.tontineId = this.tontineId
            matricule = 'M-001'
            firstName = 'Pres'; lastName = 'Ident'; phone = '+237699000001'
            it.roles = [UserRole.PRESIDENT, UserRole.MEMBER] as Set
            it.status = MemberStatus.ACTIVE
        })

        principal = new AuthenticatedUser(userId, user.email, user.phone, 'hash',
                [UserRole.PRESIDENT, UserRole.MEMBER] as Set, true)
        SecurityContextHolder.setContext(new SecurityContextImpl(
                new UsernamePasswordAuthenticationToken(principal, null, principal.authorities)))
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    // ---------- helpers ----------

    MembershipInvitation invitation(Map args = [:]) {
        def inv = invitationRepository.save(new MembershipInvitation().tap {
            it.tontineId = this.tontineId
            candidateFullName = args.name ?: 'Cand Idat'
            candidatePhone = args.phone ?: '+237699111222'
            candidateEmail = args.email
            proposedRole = (args.role ?: UserRole.MEMBER) as UserRole
            channels = args.channels ?: 'SMS'
            status = (args.status ?: InvitationStatus.SENT) as InvitationStatus
            token = args.token ?: ('tok-' + UUID.randomUUID().toString().replace('-', ''))
            expiresAt = args.expiresAt ?: Instant.now().plus(7, ChronoUnit.DAYS)
            invitedByUserId = this.userId
            invitedByFullName = 'Pres Ident'
            remindersSent = (args.reminders ?: 0) as int
        })
        if (args.invitedAt != null) {
            inv.invitedAt = args.invitedAt as Instant
            inv = invitationRepository.save(inv)
        }
        inv
    }

    Member activeMember(String phone) {
        memberRepository.save(new Member().tap {
            it.tontineId = this.tontineId
            matricule = 'M-' + UUID.randomUUID().toString().substring(0, 8)
            firstName = 'Deja'; lastName = 'Membre'; it.phone = phone
            it.roles = [UserRole.MEMBER] as Set
            it.status = MemberStatus.ACTIVE
        })
    }

    String inviteBody(Map args) {
        def channels = (args.channels ?: ['SMS', 'EMAIL']).collect { "\"${it}\"" }.join(',')
        def email = args.email == null ? '' : "\"candidateEmail\":\"${args.email}\","
        """{
            "candidateFullName":"${args.name ?: 'Jean Tagne'}",
            "candidatePhone":"${args.phone ?: '+237699000099'}",
            ${email}
            "proposedRole":"${args.role ?: 'MEMBER'}",
            "channels":[${channels}],
            "message":"${args.message ?: 'Bienvenue'}"
        }"""
    }

    // ---------- 1 ----------

    def "1 - invite : succes SMS + email, status SENT"() {
        when:
        def result = mockMvc.perform(post('/president/membership/invite')
                .header('X-Tontine-Id', tontineId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(inviteBody(phone: '+237699000099', email: 'jean@example.cm', channels: ['SMS', 'EMAIL'])))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.data.status').value('SENT'))
                .andExpect(jsonPath('$.data.candidatePhone').value('+237699000099'))
                .andExpect(jsonPath('$.data.acceptUrl').exists())
                .andExpect(jsonPath('$.data.channels').isArray())

        and:
        def all = invitationRepository.findAll()
        all.size() == 1
        all.first().status == InvitationStatus.SENT
        all.first().channels.contains('SMS')
        all.first().channels.contains('EMAIL')
    }

    // ---------- 2 ----------

    def "2 - invite : sans email, SENT via SMS uniquement"() {
        when:
        def result = mockMvc.perform(post('/president/membership/invite')
                .header('X-Tontine-Id', tontineId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(inviteBody(phone: '+237699000098', email: null, channels: ['SMS'])))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.data.status').value('SENT'))

        and:
        invitationRepository.findAll().first().channels == 'SMS'
    }

    // ---------- 3 ----------

    def "3 - invite : telephone deja membre actif -> 409 MEMBER_ALREADY_EXISTS"() {
        given:
        activeMember('+237699000097')

        expect:
        mockMvc.perform(post('/president/membership/invite')
                .header('X-Tontine-Id', tontineId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(inviteBody(phone: '+237699000097')))
                .andExpect(status().isConflict())
                .andExpect(jsonPath('$.code').value('MEMBER_ALREADY_EXISTS'))
    }

    // ---------- 4 ----------

    def "4 - invite : invitation deja en cours -> 409 INVITATION_ALREADY_PENDING"() {
        given:
        invitation(phone: '+237699000096', status: InvitationStatus.SENT)

        expect:
        mockMvc.perform(post('/president/membership/invite')
                .header('X-Tontine-Id', tontineId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(inviteBody(phone: '+237699000096')))
                .andExpect(status().isConflict())
                .andExpect(jsonPath('$.code').value('INVITATION_ALREADY_PENDING'))
    }

    // ---------- 5 ----------

    def "5 - invite : tontine pleine -> 422 TONTINE_FULL"() {
        given:
        tontine.maxMembers = 1
        tontineRepository.save(tontine)

        expect:
        mockMvc.perform(post('/president/membership/invite')
                .header('X-Tontine-Id', tontineId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(inviteBody(phone: '+237699000095')))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath('$.code').value('TONTINE_FULL'))
    }

    // ---------- 6 ----------

    def "6 - list : ordre invitedAt DESC"() {
        given:
        def older = invitation(phone: '+237699000001', token: 'tok-older',
                invitedAt: Instant.now().minus(2, ChronoUnit.HOURS))
        def newer = invitation(phone: '+237699000002', token: 'tok-newer',
                invitedAt: Instant.now().minus(5, ChronoUnit.MINUTES))

        expect:
        mockMvc.perform(get('/president/membership/invitations')
                .header('X-Tontine-Id', tontineId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data.length()').value(2))
                .andExpect(jsonPath('$.data[0].candidatePhone').value('+237699000002'))
                .andExpect(jsonPath('$.data[1].candidatePhone').value('+237699000001'))
    }

    // ---------- 7 ----------

    def "7 - resend : status SENT -> ok, remindersSent +1"() {
        given:
        def inv = invitation(status: InvitationStatus.SENT, reminders: 0)

        when:
        def result = mockMvc.perform(post("/president/membership/invitations/${inv.id}/resend")
                .header('X-Tontine-Id', tontineId.toString()))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.data.status').value('SENT'))
                .andExpect(jsonPath('$.data.remindersSent').value(1))

        and:
        invitationRepository.findById(inv.id).get().remindersSent == 1
    }

    // ---------- 8 ----------

    def "8 - resend : status ACCEPTED -> 422 INVITATION_NOT_RESENDABLE"() {
        given:
        def inv = invitation(status: InvitationStatus.ACCEPTED)

        expect:
        mockMvc.perform(post("/president/membership/invitations/${inv.id}/resend")
                .header('X-Tontine-Id', tontineId.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath('$.code').value('INVITATION_NOT_RESENDABLE'))
    }

    // ---------- 9 ----------

    def "9 - cancel : status SENT -> ok"() {
        given:
        def inv = invitation(status: InvitationStatus.SENT)

        when:
        def result = mockMvc.perform(post("/president/membership/invitations/${inv.id}/cancel")
                .header('X-Tontine-Id', tontineId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"reason":"Erreur de saisie"}'))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.data.status').value('CANCELLED'))

        and:
        invitationRepository.findById(inv.id).get().status == InvitationStatus.CANCELLED
    }

    // ---------- 10 ----------

    def "10 - cancel : status ACCEPTED -> 422 INVITATION_ALREADY_ACCEPTED"() {
        given:
        def inv = invitation(status: InvitationStatus.ACCEPTED)

        expect:
        mockMvc.perform(post("/president/membership/invitations/${inv.id}/cancel")
                .header('X-Tontine-Id', tontineId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"reason":"x"}'))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath('$.code').value('INVITATION_ALREADY_ACCEPTED'))
    }

    // ---------- 11 ----------

    def "11 - invite : non-president -> 403"() {
        given: 'le membre courant perd le role President'
        presidentMember.roles = [UserRole.MEMBER] as Set
        memberRepository.save(presidentMember)

        expect:
        mockMvc.perform(post('/president/membership/invite')
                .header('X-Tontine-Id', tontineId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(inviteBody(phone: '+237699000094')))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath('$.code').value('FORBIDDEN'))
    }
}
