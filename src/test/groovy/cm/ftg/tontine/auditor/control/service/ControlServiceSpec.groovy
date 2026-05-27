package cm.ftg.tontine.auditor.control.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.auditor.control.dto.CompleteControlRequest
import cm.ftg.tontine.auditor.control.dto.CreateControlRequest
import cm.ftg.tontine.auditor.control.entity.Control
import cm.ftg.tontine.auditor.control.entity.ControlCheckpoint
import cm.ftg.tontine.auditor.control.enums.CheckpointCategory
import cm.ftg.tontine.auditor.control.enums.ControlKind
import cm.ftg.tontine.auditor.control.enums.ControlStatus
import cm.ftg.tontine.auditor.control.repository.ControlCheckpointRepository
import cm.ftg.tontine.auditor.control.repository.ControlRepository
import cm.ftg.tontine.auditor.security.AuditorAccessChecker
import cm.ftg.tontine.common.exception.ApiException
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDate

class ControlServiceSpec extends Specification {

    ControlRepository controlRepository = Mock()
    ControlCheckpointRepository checkpointRepository = Mock()
    AuditorAccessChecker accessChecker = Mock()
    AuditService auditService = Mock()

    @Subject
    ControlService service = new ControlService(
            controlRepository, checkpointRepository, accessChecker, auditService)

    UUID tontineId = UUID.randomUUID()
    UUID userId = UUID.randomUUID()
    UUID controlId = UUID.randomUUID()

    def "create : controle initial en PLANNED + audit"() {
        given:
        controlRepository.save(_) >> { Control c -> c.id = controlId; c }
        def req = new CreateControlRequest(ControlKind.MONTHLY,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31))

        when:
        def dto = service.create(tontineId, userId, 'Audrey Tor', req)

        then:
        dto.status == ControlStatus.PLANNED
        dto.kind == ControlKind.MONTHLY
        dto.createdByFullName == 'Audrey Tor'
        dto.checkpoints.size() == 0
        1 * auditService.record(userId, 'CONTROL_CREATE', 'Control', controlId.toString(),
                tontineId, { it.contains('MONTHLY') })
    }

    def "complete : enregistre checkpoints, calcule variance et conform, marque COMPLETED"() {
        given:
        def existing = new Control().tap {
            id = controlId; it.tontineId = this.tontineId; status = ControlStatus.PLANNED
        }
        controlRepository.findById(controlId) >> Optional.of(existing)
        controlRepository.save(_) >> { it[0] }
        checkpointRepository.save(_) >> { ControlCheckpoint cp -> cp.id = UUID.randomUUID(); cp }
        def req = new CompleteControlRequest([
                new CompleteControlRequest.CheckpointInput(
                        'Caisse principale', CheckpointCategory.CASH,
                        100000.00, 100000.00, 'OK'),
                new CompleteControlRequest.CheckpointInput(
                        'Cotisations session #5', CheckpointCategory.CONTRIBUTION,
                        50000.00, 49500.00, 'manque 500 XAF'),
                new CompleteControlRequest.CheckpointInput(
                        'Prets en cours', CheckpointCategory.LOAN,
                        null, null, 'non verifiable')
        ], 'observations generales')

        when:
        def dto = service.complete(controlId, tontineId, userId, req)

        then:
        dto.status == ControlStatus.COMPLETED
        dto.observations == 'observations generales'
        dto.completedAt != null
        dto.checkpoints.size() == 3
        dto.checkpoints[0].variance == 0.00
        dto.checkpoints[0].conform        // expected==observed
        dto.checkpoints[1].variance == -500.00
        !dto.checkpoints[1].conform       // ecart non nul
        dto.checkpoints[2].variance == null
        dto.checkpoints[2].conform == null  // null si values manquantes
        1 * checkpointRepository.deleteByControlId(controlId)
        1 * auditService.record(userId, 'CONTROL_COMPLETE', 'Control', controlId.toString(),
                tontineId, { it.contains('"checkpoints":3') })
    }

    def "complete : refuse un controle deja COMPLETED"() {
        given:
        def c = new Control().tap {
            id = controlId; it.tontineId = this.tontineId; status = ControlStatus.COMPLETED
        }
        controlRepository.findById(controlId) >> Optional.of(c)

        when:
        service.complete(controlId, tontineId, userId,
                new CompleteControlRequest([
                        new CompleteControlRequest.CheckpointInput(
                                'X', CheckpointCategory.CASH, 0.00, 0.00, null)
                ], null))

        then:
        ApiException ex = thrown()
        ex.code == 'CONTROL_ALREADY_COMPLETED'
        ex.status.value() == 409
        0 * checkpointRepository.deleteByControlId(_)
    }

    def "complete : refuse un controle d'une autre tontine"() {
        given:
        def c = new Control().tap {
            id = controlId; it.tontineId = UUID.randomUUID(); status = ControlStatus.PLANNED
        }
        controlRepository.findById(controlId) >> Optional.of(c)

        when:
        service.complete(controlId, tontineId, userId,
                new CompleteControlRequest([
                        new CompleteControlRequest.CheckpointInput(
                                'X', CheckpointCategory.CASH, 0.00, 0.00, null)
                ], null))

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
    }

    def "complete : 404 quand le controle n'existe pas"() {
        given:
        controlRepository.findById(controlId) >> Optional.empty()

        when:
        service.complete(controlId, tontineId, userId,
                new CompleteControlRequest([
                        new CompleteControlRequest.CheckpointInput(
                                'X', CheckpointCategory.CASH, 0.00, 0.00, null)
                ], null))

        then:
        thrown(cm.ftg.tontine.common.exception.ResourceNotFoundException)
    }
}
