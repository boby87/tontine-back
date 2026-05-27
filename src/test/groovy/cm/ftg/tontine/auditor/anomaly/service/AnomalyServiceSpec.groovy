package cm.ftg.tontine.auditor.anomaly.service

import cm.ftg.tontine.audit.service.AuditService
import cm.ftg.tontine.auditor.anomaly.dto.CloseAnomalyRequest
import cm.ftg.tontine.auditor.anomaly.dto.CreateAnomalyRequest
import cm.ftg.tontine.auditor.anomaly.entity.Anomaly
import cm.ftg.tontine.auditor.anomaly.enums.AnomalyAudience
import cm.ftg.tontine.auditor.anomaly.enums.AnomalyCategory
import cm.ftg.tontine.auditor.anomaly.enums.AnomalySeverity
import cm.ftg.tontine.auditor.anomaly.enums.AnomalyStatus
import cm.ftg.tontine.auditor.anomaly.repository.AnomalyRepository
import cm.ftg.tontine.auditor.security.AuditorAccessChecker
import cm.ftg.tontine.common.exception.ApiException
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class AnomalyServiceSpec extends Specification {

    AnomalyRepository repository = Mock()
    AuditorAccessChecker accessChecker = Mock()
    AuditService auditService = Mock()

    @Subject
    AnomalyService service = new AnomalyService(
            repository, accessChecker, auditService)

    UUID tontineId = UUID.randomUUID()
    UUID userId = UUID.randomUUID()
    UUID anomalyId = UUID.randomUUID()

    Anomaly anomaly(AnomalyStatus status) {
        new Anomaly().tap {
            id = anomalyId
            it.tontineId = this.tontineId
            it.status = status
            category = AnomalyCategory.CASH_DISCREPANCY
            severity = AnomalySeverity.HIGH
        }
    }

    def "create : nouvelle anomalie en OPEN avec flags"() {
        given:
        repository.save(_) >> { Anomaly a -> a.id = anomalyId; a }
        def req = new CreateAnomalyRequest(AnomalyCategory.CASH_DISCREPANCY,
                AnomalySeverity.HIGH, 'Caisse en moins',
                'Ecart -500 sur la caisse principale', AnomalyAudience.BUREAU,
                Boolean.TRUE, Boolean.TRUE)

        when:
        def dto = service.create(tontineId, userId, 'Audrey Tor', req)

        then:
        dto.status == AnomalyStatus.OPEN
        dto.title == 'Caisse en moins'
        dto.severity == AnomalySeverity.HIGH
        dto.requestsResponse
        dto.copyToTreasurer
        dto.reportedByFullName == 'Audrey Tor'
        1 * auditService.record(userId, 'ANOMALY_CREATE', 'Anomaly', anomalyId.toString(),
                tontineId, { it.contains('CASH_DISCREPANCY') && it.contains('HIGH') })
    }

    def "create : flags absents (null) defaut a false"() {
        given:
        repository.save(_) >> { Anomaly a -> a.id = anomalyId; a }
        def req = new CreateAnomalyRequest(AnomalyCategory.OTHER,
                AnomalySeverity.LOW, 'x', 'y', AnomalyAudience.PRESIDENT, null, null)

        when:
        def dto = service.create(tontineId, userId, 'A', req)

        then:
        !dto.requestsResponse
        !dto.copyToTreasurer
    }

    @Unroll
    def "close : autorise depuis #status"() {
        given:
        def a = anomaly(status)
        repository.findById(anomalyId) >> Optional.of(a)
        repository.save(_) >> { it[0] }

        when:
        def dto = service.close(anomalyId, tontineId, userId,
                new CloseAnomalyRequest('resolu apres verification'))

        then:
        dto.status == AnomalyStatus.CLOSED
        a.closedAt != null
        a.resolutionComment == 'resolu apres verification'
        1 * auditService.record(userId, 'ANOMALY_CLOSE', 'Anomaly', anomalyId.toString(),
                tontineId, null)

        where:
        status << [AnomalyStatus.OPEN, AnomalyStatus.IN_RESPONSE, AnomalyStatus.RESOLVED]
    }

    def "close : refuse depuis CLOSED"() {
        given:
        def a = anomaly(AnomalyStatus.CLOSED)
        repository.findById(anomalyId) >> Optional.of(a)

        when:
        service.close(anomalyId, tontineId, userId, new CloseAnomalyRequest(null))

        then:
        ApiException ex = thrown()
        ex.code == 'ANOMALY_INVALID_STATE'
    }

    def "close : ne touche pas resolutionComment si request null"() {
        given:
        def a = anomaly(AnomalyStatus.OPEN).tap { resolutionComment = 'initial' }
        repository.findById(anomalyId) >> Optional.of(a)
        repository.save(_) >> { it[0] }

        when:
        service.close(anomalyId, tontineId, userId, null)

        then:
        a.status == AnomalyStatus.CLOSED
        a.resolutionComment == 'initial'
    }

    @Unroll
    def "reopen : autorise depuis #status"() {
        given:
        def a = anomaly(status).tap { closedAt = java.time.Instant.now() }
        repository.findById(anomalyId) >> Optional.of(a)
        repository.save(_) >> { it[0] }

        when:
        def dto = service.reopen(anomalyId, tontineId, userId)

        then:
        dto.status == AnomalyStatus.OPEN
        a.closedAt == null
        a.resolvedAt == null

        where:
        status << [AnomalyStatus.CLOSED, AnomalyStatus.RESOLVED]
    }

    @Unroll
    def "reopen : refuse depuis #status"() {
        given:
        def a = anomaly(status)
        repository.findById(anomalyId) >> Optional.of(a)

        when:
        service.reopen(anomalyId, tontineId, userId)

        then:
        ApiException ex = thrown()
        ex.code == 'ANOMALY_INVALID_STATE'

        where:
        status << [AnomalyStatus.OPEN, AnomalyStatus.IN_RESPONSE]
    }

    def "close : refuse une anomalie d'une autre tontine"() {
        given:
        def a = anomaly(AnomalyStatus.OPEN).tap { it.tontineId = UUID.randomUUID() }
        repository.findById(anomalyId) >> Optional.of(a)

        when:
        service.close(anomalyId, tontineId, userId, new CloseAnomalyRequest(null))

        then:
        ApiException ex = thrown()
        ex.code == 'FORBIDDEN'
    }
}
