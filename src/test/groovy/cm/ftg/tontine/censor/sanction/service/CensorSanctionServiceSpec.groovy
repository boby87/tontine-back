package cm.ftg.tontine.censor.sanction.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.censor.sanction.dto.ApplySanctionRequest
import cm.ftg.tontine.censor.sanction.dto.CancelSanctionRequest
import cm.ftg.tontine.censor.sanction.dto.ConfirmBatchRequest
import cm.ftg.tontine.censor.security.CensorAccessChecker
import cm.ftg.tontine.common.exception.ApiException
import cm.ftg.tontine.member.entity.Member
import cm.ftg.tontine.member.repository.MemberRepository
import cm.ftg.tontine.president.sanction.entity.Sanction
import cm.ftg.tontine.president.sanction.enums.SanctionCancelByRole
import cm.ftg.tontine.president.sanction.enums.SanctionSeverity
import cm.ftg.tontine.president.sanction.enums.SanctionStatus
import cm.ftg.tontine.president.sanction.enums.SanctionType
import cm.ftg.tontine.president.sanction.repository.SanctionRepository
import cm.ftg.tontine.tontine.entity.Tontine
import cm.ftg.tontine.tontine.entity.TontineRulesEmbeddable
import cm.ftg.tontine.tontine.repository.TontineRepository
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class CensorSanctionServiceSpec extends Specification {

    SanctionRepository sanctionRepository = Mock()
    MemberRepository memberRepository = Mock()
    TontineRepository tontineRepository = Mock()
    CensorAccessChecker accessChecker = Mock()
    AuditService auditService = Mock()

    @Subject
    CensorSanctionService service = new CensorSanctionService(
            sanctionRepository, memberRepository, tontineRepository, accessChecker, auditService)

    UUID tontineId = UUID.randomUUID()
    UUID userId = UUID.randomUUID()
    UUID memberId = UUID.randomUUID()
    UUID sanctionId = UUID.randomUUID()

    Member censorMember = new Member().tap { firstName = 'Cen'; lastName = 'Seur' }
    Member targetMember(UUID tId = tontineId) {
        new Member().tap {
            id = memberId; it.tontineId = tId
            firstName = 'Alice'; lastName = 'Member'
        }
    }

    Tontine tontineWithRules() {
        new Tontine().tap {
            id = tontineId
            rules = new TontineRulesEmbeddable(
                    1000.00,    // late penalty (lateness)
                    5000.00,    // absence penalty
                    500.00,     // contribution late penalty
                    0.00, 0.00, 12,
                    50000.00,
                    0.00, 0.00)
        }
    }

    def setup() {
        accessChecker.requireCensor(userId, tontineId) >> censorMember
    }

    @Unroll
    def "apply : derive le montant des regles quand amount est null (type #type => #expectedAmount)"() {
        given:
        memberRepository.findById(memberId) >> Optional.of(targetMember())
        tontineRepository.findById(tontineId) >> Optional.of(tontineWithRules())
        sanctionRepository.save(_) >> { Sanction s -> s.id = sanctionId; s }

        when:
        def dto = service.apply(tontineId, userId, new ApplySanctionRequest(
                memberId, type, null, 'reason', SanctionSeverity.MEDIUM, null, true, null))

        then:
        dto.amount == expectedAmount
        dto.status == SanctionStatus.PENDING
        !dto.autoDetected
        1 * auditService.record(userId, 'CENSOR_SANCTION_APPLY', 'Sanction', sanctionId.toString(),
                tontineId, _)

        where:
        type                              | expectedAmount
        SanctionType.ABSENCE              | 5000.00
        SanctionType.LATENESS             | 1000.00
        SanctionType.CONTRIBUTION_LATE    | 500.00
    }

    def "apply : utilise l'amount explicite quand fourni"() {
        given:
        memberRepository.findById(memberId) >> Optional.of(targetMember())
        tontineRepository.findById(tontineId) >> Optional.of(tontineWithRules())
        sanctionRepository.save(_) >> { Sanction s -> s.id = sanctionId; s }

        when:
        def dto = service.apply(tontineId, userId, new ApplySanctionRequest(
                memberId, SanctionType.DISCIPLINE, 7500.00, 'motif',
                SanctionSeverity.HIGH, 'comportement', true, null))

        then:
        dto.amount == 7500.00
        dto.customLabel == 'comportement'
        dto.severity == SanctionSeverity.HIGH
    }

    def "apply : refuse une sanction financiere sans amount derivable"() {
        given:
        memberRepository.findById(memberId) >> Optional.of(targetMember())
        tontineRepository.findById(tontineId) >> Optional.of(tontineWithRules())

        when:
        // OTHER ne mappe sur aucune regle ; financial=true + amount null => erreur
        service.apply(tontineId, userId, new ApplySanctionRequest(
                memberId, SanctionType.OTHER, null, 'motif',
                SanctionSeverity.LOW, null, true, null))

        then:
        ApiException ex = thrown()
        ex.code == 'SANCTION_AMOUNT_REQUIRED'
        ex.status.value() == 422
        0 * sanctionRepository.save(_)
    }

    def "apply : sanction non financiere sans amount donne 0"() {
        given:
        memberRepository.findById(memberId) >> Optional.of(targetMember())
        tontineRepository.findById(tontineId) >> Optional.of(tontineWithRules())
        sanctionRepository.save(_) >> { Sanction s -> s.id = sanctionId; s }

        when:
        def dto = service.apply(tontineId, userId, new ApplySanctionRequest(
                memberId, SanctionType.DISCIPLINE, null, 'motif',
                SanctionSeverity.LOW, 'avertissement', false, null))

        then:
        dto.amount == 0.00
        !dto.financial
    }

    def "apply : refuse un membre hors tontine"() {
        given:
        memberRepository.findById(memberId) >> Optional.of(targetMember(UUID.randomUUID()))
        tontineRepository.findById(tontineId) >> Optional.of(tontineWithRules())

        when:
        service.apply(tontineId, userId, new ApplySanctionRequest(
                memberId, SanctionType.ABSENCE, null, 'r', null, null, true, null))

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
    }

    def "confirmBatch : compte les confirmes, ignore les non-PENDING et hors tontine"() {
        given:
        UUID idA = UUID.randomUUID()    // PENDING -> confirmed
        UUID idB = UUID.randomUUID()    // deja CONFIRMED -> skipped
        UUID idC = UUID.randomUUID()    // hors tontine -> skipped
        UUID idD = UUID.randomUUID()    // inexistant -> skipped
        def sA = new Sanction().tap { id = idA; it.tontineId = this.tontineId; status = SanctionStatus.PENDING }
        def sB = new Sanction().tap { id = idB; it.tontineId = this.tontineId; status = SanctionStatus.CONFIRMED }
        def sC = new Sanction().tap { id = idC; it.tontineId = UUID.randomUUID(); status = SanctionStatus.PENDING }
        sanctionRepository.findById(idA) >> Optional.of(sA)
        sanctionRepository.findById(idB) >> Optional.of(sB)
        sanctionRepository.findById(idC) >> Optional.of(sC)
        sanctionRepository.findById(idD) >> Optional.empty()
        sanctionRepository.save(_) >> { it[0] }

        when:
        def result = service.confirmBatch(tontineId, userId,
                new ConfirmBatchRequest([idA, idB, idC, idD]))

        then:
        result.confirmedCount() == 1
        result.skippedCount() == 3
        result.total() == 4
        sA.status == SanctionStatus.CONFIRMED
        sA.resolvedByUserId == userId
        1 * auditService.record(userId, 'CENSOR_SANCTION_CONFIRM_BATCH', 'Sanction', null,
                tontineId, { it.contains('"confirmed":1') && it.contains('"skipped":3') })
    }

    def "cancel : passe en CANCELLED avec audit"() {
        given:
        def s = new Sanction().tap {
            id = sanctionId; it.tontineId = this.tontineId; status = SanctionStatus.PENDING
        }
        sanctionRepository.findById(sanctionId) >> Optional.of(s)
        sanctionRepository.save(_) >> { it[0] }

        when:
        def dto = service.cancel(sanctionId, tontineId, userId,
                new CancelSanctionRequest('saisie erronee'))

        then:
        dto.status == SanctionStatus.CANCELLED
        s.cancelledByRole == SanctionCancelByRole.CENSOR
        s.cancelledByUserId == userId
        s.cancelReason == 'saisie erronee'
        1 * auditService.record(userId, 'CENSOR_SANCTION_CANCEL', 'Sanction',
                sanctionId.toString(), tontineId, { it.contains('saisie erronee') })
    }

    def "cancel : refuse une sanction payee (refund requis)"() {
        given:
        def s = new Sanction().tap {
            id = sanctionId; it.tontineId = this.tontineId; status = SanctionStatus.PAID
        }
        sanctionRepository.findById(sanctionId) >> Optional.of(s)

        when:
        service.cancel(sanctionId, tontineId, userId, new CancelSanctionRequest('x'))

        then:
        ApiException ex = thrown()
        ex.code == 'SANCTION_PAID_REFUND_REQUIRED'
        ex.status.value() == 409
    }

    @Unroll
    def "cancel : refuse un etat final #status"() {
        given:
        def s = new Sanction().tap {
            id = sanctionId; it.tontineId = this.tontineId; it.status = status
        }
        sanctionRepository.findById(sanctionId) >> Optional.of(s)

        when:
        service.cancel(sanctionId, tontineId, userId, new CancelSanctionRequest('x'))

        then:
        ApiException ex = thrown()
        ex.code == 'SANCTION_FINAL_STATE'

        where:
        status << [SanctionStatus.CANCELLED, SanctionStatus.WAIVED]
    }
}
