package cm.ftg.tontine.auditor.session.service;

import cm.ftg.tontine.auditor.security.AuditorAccessChecker;
import cm.ftg.tontine.president.session.repository.SessionRepository;
import cm.ftg.tontine.treasurer.session.dto.SessionSummaryDto;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditorSessionService {

    private final SessionRepository sessionRepository;
    private final AuditorAccessChecker accessChecker;

    public AuditorSessionService(SessionRepository sessionRepository,
                                 AuditorAccessChecker accessChecker) {
        this.sessionRepository = sessionRepository;
        this.accessChecker = accessChecker;
    }

    @Transactional(readOnly = true)
    public List<SessionSummaryDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireAuditor(userId, tontineId);
        return sessionRepository.findByTontineIdOrderByNumberAsc(tontineId).stream()
                .map(SessionSummaryDto::from)
                .toList();
    }
}
