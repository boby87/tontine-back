package cm.ftg.tontine.president.presidencytransfer.controller

import cm.ftg.tontine.auth.entity.UserEntity
import cm.ftg.tontine.auth.repository.UserRepository
import cm.ftg.tontine.common.enums.ContributionFrequency
import cm.ftg.tontine.common.enums.MemberStatus
import cm.ftg.tontine.common.enums.TontineStatus
import cm.ftg.tontine.common.enums.UserRole
import cm.ftg.tontine.member.entity.Member
import cm.ftg.tontine.member.repository.MemberRepository
import cm.ftg.tontine.president.presidencytransfer.entity.PresidencyTransfer
import cm.ftg.tontine.president.presidencytransfer.enums.PresidencyTransferStatus
import cm.ftg.tontine.president.presidencytransfer.repository.PresidencyTransferRepository
import cm.ftg.tontine.president.presidencytransfer.service.PresidencyAcceptService
import cm.ftg.tontine.scheduling.PresidencyTransferExpirationJob
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

import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
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
class PresidencyTransferControllerIT extends Specification {

    @Autowired
    MockMvc mockMvc
    @Autowired
    PresidencyTransferRepository transferRepository
    @Autowired
    MemberRepository memberRepository
    @Autowired
    UserRepository userRepository
    @Autowired
    TontineRepository tontineRepository
    @Autowired
    PresidencyAcceptService acceptService
    @Autowired
    PresidencyTransferExpirationJob expirationJob

    UUID tontineId
    UUID presidentUserId
    UUID presidentMemberId
    UUID targetUserId
    UUID targetMemberId

    def setup() {
        transferRepository.deleteAll()
        memberRepository.deleteAll()
        userRepository.deleteAll()
        tontineRepository.deleteAll()

        def t = tontineRepository.save(new Tontine().tap {
            name = 'Tontine PT'
            status = TontineStatus.ACTIVE
            contributionAmount = BigDecimal.valueOf(10000)
            frequency = ContributionFrequency.MONTHLY
            startDate = LocalDate.now()
            maxMembers = 10
            createdByUserId = UUID.randomUUID()
            totalSaved = BigDecimal.ZERO
            memberCount = 2
            rules = new TontineRulesDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.valueOf(100000), BigDecimal.valueOf(5), 12,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO).toEmbeddable()
        })
        tontineId = t.id

        def pres = newUser('pres', [UserRole.PRESIDENT, UserRole.MEMBER] as Set)
        presidentUserId = pres.id
        presidentMemberId = newMember(pres, [UserRole.PRESIDENT, UserRole.MEMBER] as Set, MemberStatus.ACTIVE).id

        def target = newUser('target', [UserRole.MEMBER] as Set)
        targetUserId = target.id
        targetMemberId = newMember(target, [UserRole.MEMBER] as Set, MemberStatus.ACTIVE).id

        authAs(presidentUserId)
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    // ---------- helpers ----------

    UserEntity newUser(String prefix, Set<UserRole> roles) {
        def suffix = UUID.randomUUID().toString().replace('-', '').substring(0, 10)
        userRepository.save(new UserEntity().tap {
            firstName = prefix.capitalize(); lastName = 'User'
            email = "${prefix}_${suffix}@example.com"
            phone = "+23769${suffix.substring(0, 7)}"
            passwordHash = 'hash'
            it.roles = roles
            active = true
        })
    }

    Member newMember(UserEntity user, Set<UserRole> roles, MemberStatus status) {
        memberRepository.save(new Member().tap {
            it.userId = user.id
            it.tontineId = this.tontineId
            matricule = 'M-' + UUID.randomUUID().toString().substring(0, 8)
            firstName = user.firstName; lastName = user.lastName; phone = user.phone
            it.roles = roles
            it.status = status
        })
    }

    void authAs(UUID userId) {
        def principal = new AuthenticatedUser(userId, 'x@example.com', '+237600000000', 'h',
                [UserRole.MEMBER] as Set, true)
        SecurityContextHolder.setContext(new SecurityContextImpl(
                new UsernamePasswordAuthenticationToken(principal, null, principal.authorities)))
    }

    PresidencyTransfer seedTransfer(Map args = [:]) {
        def saved = transferRepository.save(new PresidencyTransfer().tap {
            it.tontineId = this.tontineId
            initiatedByUserId = args.initiatedBy ?: this.presidentUserId
            initiatedByFullName = 'Pres User'
            targetMemberId = args.targetMemberId ?: this.targetMemberId
            targetUserId = args.containsKey('targetUserId') ? args.targetUserId : this.targetUserId
            targetMemberFullName = 'Target User'
            reason = args.reason ?: 'Motif de passation suffisamment long'
            status = (args.status ?: PresidencyTransferStatus.PENDING) as PresidencyTransferStatus
            expiresAt = args.expiresAt ?: Instant.now().plus(72, ChronoUnit.HOURS)
            if (args.status == PresidencyTransferStatus.ACCEPTED) {
                acceptedAt = Instant.now()
            }
        })
        if (args.initiatedAt != null) {
            saved.initiatedAt = args.initiatedAt as Instant
            saved = transferRepository.save(saved)
        }
        saved
    }

    String initiateBody(UUID memberId, String reason = 'Je passe la main pour raisons personnelles') {
        "{\"targetMemberId\":\"${memberId}\",\"reason\":\"${reason}\"}"
    }

    // ---------- 1 ----------

    def "1 - president initie un transfert vers un membre ACTIVE -> 200 + DB"() {
        when:
        def result = mockMvc.perform(post('/president/presidency-transfer')
                .header('X-Tontine-Id', tontineId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(initiateBody(targetMemberId)))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.data.status').value('PENDING'))
                .andExpect(jsonPath('$.data.targetMemberId').value(targetMemberId.toString()))
                .andExpect(jsonPath('$.data.expiresAt').exists())

        and:
        def all = transferRepository.findAll()
        all.size() == 1
        all.first().status == PresidencyTransferStatus.PENDING
        all.first().targetUserId == targetUserId
    }

    // ---------- 2 ----------

    def "2 - cible = soi-meme -> 422 TARGET_CANNOT_BE_SELF"() {
        expect:
        mockMvc.perform(post('/president/presidency-transfer')
                .header('X-Tontine-Id', tontineId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(initiateBody(presidentMemberId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath('$.code').value('TARGET_CANNOT_BE_SELF'))
    }

    // ---------- 3 ----------

    def "3 - cible SUSPENDED -> 422 TARGET_MEMBER_NOT_ELIGIBLE"() {
        given:
        def target = memberRepository.findById(targetMemberId).get()
        target.status = MemberStatus.SUSPENDED
        memberRepository.save(target)

        expect:
        mockMvc.perform(post('/president/presidency-transfer')
                .header('X-Tontine-Id', tontineId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(initiateBody(targetMemberId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath('$.code').value('TARGET_MEMBER_NOT_ELIGIBLE'))
    }

    // ---------- 4 ----------

    def "4 - deux transferts simultanes sur la meme tontine -> 409 ALREADY_PENDING"() {
        given:
        mockMvc.perform(post('/president/presidency-transfer')
                .header('X-Tontine-Id', tontineId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(initiateBody(targetMemberId)))
                .andExpect(status().isOk())

        expect:
        mockMvc.perform(post('/president/presidency-transfer')
                .header('X-Tontine-Id', tontineId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(initiateBody(targetMemberId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath('$.code').value('PRESIDENCY_TRANSFER_ALREADY_PENDING'))
    }

    // ---------- 5 ----------

    def "5 - membre cible appelle GET /pending -> renvoie le transfert"() {
        given:
        def transfer = seedTransfer()
        authAs(targetUserId)

        expect:
        mockMvc.perform(get('/members/me/presidency-transfer/pending'))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data.id').value(transfer.id.toString()))
                .andExpect(jsonPath('$.data.status').value('PENDING'))
    }

    // ---------- 6 ----------

    def "6 - autre membre appelle GET /pending -> data null"() {
        given:
        seedTransfer()
        def other = newUser('other', [UserRole.MEMBER] as Set)
        newMember(other, [UserRole.MEMBER] as Set, MemberStatus.ACTIVE)
        authAs(other.id)

        expect:
        mockMvc.perform(get('/members/me/presidency-transfer/pending'))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data').doesNotExist())
    }

    // ---------- 7 ----------

    def "7 - accept atomique : swap des roles, un seul President au final"() {
        given:
        def transfer = seedTransfer()
        authAs(targetUserId)

        when:
        mockMvc.perform(post("/members/me/presidency-transfer/${transfer.id}/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data.status').value('ACCEPTED'))

        then:
        def oldPres = memberRepository.findById(presidentMemberId).get()
        def newPres = memberRepository.findById(targetMemberId).get()
        !oldPres.roles.contains(UserRole.PRESIDENT)
        oldPres.roles.contains(UserRole.MEMBER)
        newPres.roles.contains(UserRole.PRESIDENT)

        and:
        def presidents = memberRepository.findByTontineIdAndRole(tontineId, UserRole.PRESIDENT)
        presidents.size() == 1
        presidents.first().id == targetMemberId
    }

    // ---------- 8 ----------

    def "8 - accept par un autre user que le destinataire -> 403"() {
        given:
        def transfer = seedTransfer()
        def other = newUser('other', [UserRole.MEMBER] as Set)
        newMember(other, [UserRole.MEMBER] as Set, MemberStatus.ACTIVE)
        authAs(other.id)

        expect:
        mockMvc.perform(post("/members/me/presidency-transfer/${transfer.id}/accept"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath('$.code').value('FORBIDDEN'))
    }

    // ---------- 9 ----------

    def "9 - accept expire -> 422 TRANSFER_EXPIRED + status EXPIRED"() {
        given:
        def transfer = seedTransfer(expiresAt: Instant.now().minus(1, ChronoUnit.HOURS))
        authAs(targetUserId)

        when:
        mockMvc.perform(post("/members/me/presidency-transfer/${transfer.id}/accept"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath('$.code').value('TRANSFER_EXPIRED'))

        then:
        transferRepository.findById(transfer.id).get().status == PresidencyTransferStatus.EXPIRED
    }

    // ---------- 10 ----------

    def "10 - decline avec motif -> status DECLINED"() {
        given:
        def transfer = seedTransfer()
        authAs(targetUserId)

        when:
        mockMvc.perform(post("/members/me/presidency-transfer/${transfer.id}/decline")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"reason":"Je ne suis pas disponible actuellement"}'))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data.status').value('DECLINED'))

        then:
        transferRepository.findById(transfer.id).get().status == PresidencyTransferStatus.DECLINED
    }

    // ---------- 11 ----------

    def "11 - cancel PENDING -> status CANCELLED"() {
        given:
        def transfer = seedTransfer()

        when:
        mockMvc.perform(post("/president/presidency-transfers/${transfer.id}/cancel")
                .header('X-Tontine-Id', tontineId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"reason":"Changement de decision"}'))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data.status').value('CANCELLED'))

        then:
        transferRepository.findById(transfer.id).get().status == PresidencyTransferStatus.CANCELLED
    }

    // ---------- 12 ----------

    def "12 - cancel ACCEPTED -> 422 TRANSFER_NOT_PENDING"() {
        given:
        def transfer = seedTransfer(status: PresidencyTransferStatus.ACCEPTED)

        expect:
        mockMvc.perform(post("/president/presidency-transfers/${transfer.id}/cancel")
                .header('X-Tontine-Id', tontineId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"reason":"x"}'))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath('$.code').value('TRANSFER_NOT_PENDING'))
    }

    // ---------- 13 ----------

    def "13 - job d'expiration : transfert vieux passe en EXPIRED"() {
        given:
        def transfer = seedTransfer(expiresAt: Instant.now().minus(1, ChronoUnit.HOURS))

        when:
        expirationJob.expirePendingTransfers()

        then:
        transferRepository.findById(transfer.id).get().status == PresidencyTransferStatus.EXPIRED
    }

    // ---------- 14 ----------

    def "14 - deux accepts concurrents -> un seul reussit (optimistic locking)"() {
        given:
        def transfer = seedTransfer()
        def executor = Executors.newFixedThreadPool(2)
        def barrier = new CyclicBarrier(2)
        def success = new AtomicInteger(0)
        def failure = new AtomicInteger(0)

        when:
        def task = {
            try {
                barrier.await()
                acceptService.accept(transfer.id, targetUserId)
                success.incrementAndGet()
            } catch (Exception ignored) {
                failure.incrementAndGet()
            }
        } as Callable
        def f1 = executor.submit(task)
        def f2 = executor.submit(task)
        f1.get()
        f2.get()
        executor.shutdown()

        then: 'invariant de surete garanti par le verrouillage optimiste @Version'
        success.get() + failure.get() == 2
        failure.get() >= 1                                  // la concurrence est detectee
        success.get() <= 1                                  // jamais deux acceptations
        memberRepository.findByTontineIdAndRole(tontineId, UserRole.PRESIDENT).size() == 1
        // coherence : le transfert est ACCEPTED si et seulement si une acceptation a reussi
        (transferRepository.findById(transfer.id).get().status == PresidencyTransferStatus.ACCEPTED) ==
                (success.get() == 1)
    }

    // ---------- 15 (extra) ----------

    def "15 - initier par un non-president -> 403"() {
        given:
        authAs(targetUserId)

        expect:
        mockMvc.perform(post('/president/presidency-transfer')
                .header('X-Tontine-Id', tontineId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(initiateBody(presidentMemberId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath('$.code').value('FORBIDDEN'))
    }

    // ---------- 16 (extra) ----------

    def "16 - cible deja President -> 422 TARGET_ALREADY_PRESIDENT"() {
        given:
        def coPres = newUser('copres', [UserRole.PRESIDENT, UserRole.MEMBER] as Set)
        def coPresMember = newMember(coPres, [UserRole.PRESIDENT, UserRole.MEMBER] as Set, MemberStatus.ACTIVE)

        expect:
        mockMvc.perform(post('/president/presidency-transfer')
                .header('X-Tontine-Id', tontineId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(initiateBody(coPresMember.id)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath('$.code').value('TARGET_ALREADY_PRESIDENT'))
    }

    // ---------- 17 (extra) ----------

    def "17 - cible introuvable -> 422 TARGET_MEMBER_NOT_ELIGIBLE"() {
        expect:
        mockMvc.perform(post('/president/presidency-transfer')
                .header('X-Tontine-Id', tontineId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(initiateBody(UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath('$.code').value('TARGET_MEMBER_NOT_ELIGIBLE'))
    }

    // ---------- 18 (extra) ----------

    def "18 - list : ordre initiatedAt DESC"() {
        given:
        def older = seedTransfer(status: PresidencyTransferStatus.CANCELLED,
                initiatedAt: Instant.now().minus(2, ChronoUnit.HOURS))
        def newer = seedTransfer(status: PresidencyTransferStatus.DECLINED,
                initiatedAt: Instant.now().minus(5, ChronoUnit.MINUTES))

        expect:
        mockMvc.perform(get('/president/presidency-transfers')
                .header('X-Tontine-Id', tontineId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data.length()').value(2))
                .andExpect(jsonPath('$.data[0].id').value(newer.id.toString()))
                .andExpect(jsonPath('$.data[1].id').value(older.id.toString()))
    }

    // ---------- 19 (extra) ----------

    def "19 - GET /pending expire -> null et bascule EXPIRED"() {
        given:
        def transfer = seedTransfer(expiresAt: Instant.now().minus(1, ChronoUnit.HOURS))
        authAs(targetUserId)

        when:
        mockMvc.perform(get('/members/me/presidency-transfer/pending'))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data').doesNotExist())

        then:
        transferRepository.findById(transfer.id).get().status == PresidencyTransferStatus.EXPIRED
    }

    // ---------- 20 (extra) ----------

    def "20 - decline par un non-destinataire -> 403"() {
        given:
        def transfer = seedTransfer()
        def other = newUser('other', [UserRole.MEMBER] as Set)
        newMember(other, [UserRole.MEMBER] as Set, MemberStatus.ACTIVE)
        authAs(other.id)

        expect:
        mockMvc.perform(post("/members/me/presidency-transfer/${transfer.id}/decline")
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"reason":"Tentative non autorisee"}'))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath('$.code').value('FORBIDDEN'))
    }

    // ---------- 21 (extra) ----------

    def "21 - cancel transfert introuvable -> 404 PRESIDENCY_TRANSFER_NOT_FOUND"() {
        expect:
        mockMvc.perform(post("/president/presidency-transfers/${UUID.randomUUID()}/cancel")
                .header('X-Tontine-Id', tontineId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content('{"reason":"x"}'))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath('$.code').value('PRESIDENCY_TRANSFER_NOT_FOUND'))
    }
}
