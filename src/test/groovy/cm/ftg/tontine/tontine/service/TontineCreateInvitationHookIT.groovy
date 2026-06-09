package cm.ftg.tontine.tontine.service

import cm.ftg.tontine.auth.entity.UserEntity
import cm.ftg.tontine.auth.repository.UserRepository
import cm.ftg.tontine.common.enums.ContributionFrequency
import cm.ftg.tontine.common.enums.UserRole
import cm.ftg.tontine.member.repository.MemberRepository
import cm.ftg.tontine.president.membership.invitation.enums.InvitationStatus
import cm.ftg.tontine.president.membership.invitation.repository.MembershipInvitationRepository
import cm.ftg.tontine.tontine.dto.CreateTontineRequest
import cm.ftg.tontine.tontine.dto.FounderInviteDto
import cm.ftg.tontine.tontine.dto.TontineRulesDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import spock.lang.Specification

import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class TontineCreateInvitationHookIT extends Specification {

    @Autowired
    TontineService tontineService
    @Autowired
    UserRepository userRepository
    @Autowired
    MemberRepository memberRepository
    @Autowired
    MembershipInvitationRepository invitationRepository

    UUID creatorId
    String creatorPhone = '+237699000001'

    def setup() {
        invitationRepository.deleteAll()
        memberRepository.deleteAll()
        userRepository.deleteAll()

        def creator = userRepository.save(new UserEntity().tap {
            firstName = 'Paul'; lastName = 'Createur'
            email = "createur_${UUID.randomUUID().toString().substring(0, 8)}@example.com"
            phone = creatorPhone
            passwordHash = 'hash'
            roles = [UserRole.MEMBER] as Set
            active = true
        })
        creatorId = creator.id
    }

    TontineRulesDto rules() {
        new TontineRulesDto(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.valueOf(100000), BigDecimal.valueOf(5),
                12, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
    }

    CreateTontineRequest request(List<FounderInviteDto> founders) {
        new CreateTontineRequest('Ma Tontine', 'desc', LocalDate.now(),
                BigDecimal.valueOf(10000), ContributionFrequency.MONTHLY, 10, rules(), founders)
    }

    // ---------- 20 ----------

    def "20 - creation avec 3 founders (1 createur + 2 autres) -> 2 invitations SENT"() {
        given:
        def founders = [
                new FounderInviteDto('Paul Createur', creatorPhone, null, UserRole.PRESIDENT),
                new FounderInviteDto('Alice Membre', '+237699000002', 'alice@example.cm', UserRole.MEMBER),
                new FounderInviteDto('Bob Tresorier', '+237699000003', null, UserRole.TREASURER)
        ]

        when:
        def dto = tontineService.create(creatorId, request(founders))

        then:
        def invitations = invitationRepository.findByTontineIdOrderByInvitedAtDesc(dto.id())
        invitations.size() == 2
        invitations.every { it.status == InvitationStatus.SENT }
        invitations.every { it.channels.contains('SMS') }
        invitations*.candidatePhone as Set == ['+237699000002', '+237699000003'] as Set
    }

    // ---------- 21 ----------

    def "21 - founder avec meme telephone que le createur -> ignore (pas d'invitation)"() {
        given:
        def founders = [
                new FounderInviteDto('Paul Createur', creatorPhone, null, UserRole.MEMBER),
                new FounderInviteDto('Alice Membre', '+237699000002', null, UserRole.MEMBER)
        ]

        when:
        def dto = tontineService.create(creatorId, request(founders))

        then:
        def invitations = invitationRepository.findByTontineIdOrderByInvitedAtDesc(dto.id())
        invitations.size() == 1
        invitations.first().candidatePhone == '+237699000002'
        invitations.every { it.candidatePhone != creatorPhone }
    }
}
