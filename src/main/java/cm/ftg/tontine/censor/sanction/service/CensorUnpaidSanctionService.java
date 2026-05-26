package cm.ftg.tontine.censor.sanction.service;

import cm.ftg.tontine.censor.sanction.dto.UnpaidSanctionDto;
import cm.ftg.tontine.censor.security.CensorAccessChecker;
import cm.ftg.tontine.president.sanction.entity.Sanction;
import cm.ftg.tontine.president.sanction.enums.SanctionStatus;
import cm.ftg.tontine.president.sanction.repository.SanctionRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CensorUnpaidSanctionService {

    private final SanctionRepository sanctionRepository;
    private final CensorAccessChecker accessChecker;

    public CensorUnpaidSanctionService(SanctionRepository sanctionRepository,
                                       CensorAccessChecker accessChecker) {
        this.sanctionRepository = sanctionRepository;
        this.accessChecker = accessChecker;
    }

    @Transactional(readOnly = true)
    public List<UnpaidSanctionDto> listUnpaid(UUID tontineId, UUID userId) {
        accessChecker.requireCensor(userId, tontineId);
        Instant now = Instant.now();
        return sanctionRepository
                .findByTontineIdAndStatusInOrderByIssuedAtDesc(tontineId, List.of(SanctionStatus.CONFIRMED))
                .stream()
                .filter(Sanction::isFinancial)
                .sorted(Comparator.comparing(Sanction::getIssuedAt))
                .map(s -> UnpaidSanctionDto.from(s, now))
                .toList();
    }
}
