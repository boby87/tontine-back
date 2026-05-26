package cm.ftg.tontine.president.emergencyblock.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auth.entity.UserEntity;
import cm.ftg.tontine.auth.repository.UserRepository;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.president.emergencyblock.dto.CreateEmergencyBlockRequest;
import cm.ftg.tontine.president.emergencyblock.dto.EmergencyBlockDto;
import cm.ftg.tontine.president.emergencyblock.dto.LiftEmergencyBlockRequest;
import cm.ftg.tontine.president.emergencyblock.entity.EmergencyBlock;
import cm.ftg.tontine.president.emergencyblock.enums.EmergencyBlockStatus;
import cm.ftg.tontine.president.emergencyblock.repository.EmergencyBlockRepository;
import cm.ftg.tontine.president.security.PresidentAccessChecker;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmergencyBlockService {

    private final EmergencyBlockRepository repository;
    private final PresidentAccessChecker accessChecker;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public EmergencyBlockService(EmergencyBlockRepository repository,
                                 PresidentAccessChecker accessChecker,
                                 AuditService auditService,
                                 UserRepository userRepository) {
        this.repository = repository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<EmergencyBlockDto> list(UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        return repository.findByTontineIdOrdered(tontineId).stream()
                .map(EmergencyBlockDto::from)
                .toList();
    }

    @Transactional
    public EmergencyBlockDto activate(UUID tontineId, UUID userId, CreateEmergencyBlockRequest req) {
        accessChecker.requirePresident(userId, tontineId);
        UserEntity actor = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND",
                        "Utilisateur introuvable", HttpStatus.NOT_FOUND));
        EmergencyBlock b = new EmergencyBlock();
        b.setTontineId(tontineId);
        b.setTarget(req.target());
        b.setTargetRef(req.targetRef());
        b.setReason(req.reason());
        b.setStatus(EmergencyBlockStatus.ACTIVE);
        b.setActivatedByUserId(userId);
        b.setActivatedByFullName(buildFullName(actor));
        EmergencyBlock saved = repository.save(b);
        auditService.record(userId, "EMERGENCY_BLOCK_ACTIVATE", "EmergencyBlock", saved.getId().toString(),
                tontineId, "{\"target\":\"" + req.target().name() + "\"}");
        return EmergencyBlockDto.from(saved);
    }

    @Transactional
    public EmergencyBlockDto lift(UUID id, UUID tontineId, UUID userId, LiftEmergencyBlockRequest req) {
        accessChecker.requirePresident(userId, tontineId);
        EmergencyBlock b = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmergencyBlock", id));
        if (!b.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        if (b.getStatus() != EmergencyBlockStatus.ACTIVE) {
            throw new ApiException("EMERGENCY_BLOCK_INVALID_STATE",
                    "Seul un blocage actif peut etre leve", HttpStatus.CONFLICT);
        }
        b.setStatus(EmergencyBlockStatus.LIFTED);
        b.setLiftedByUserId(userId);
        b.setLiftedAt(Instant.now());
        b.setLiftReason(req.reason());
        EmergencyBlock saved = repository.save(b);
        auditService.record(userId, "EMERGENCY_BLOCK_LIFT", "EmergencyBlock", id.toString(), tontineId,
                "{\"reason\":\"" + escape(req.reason()) + "\"}");
        return EmergencyBlockDto.from(saved);
    }

    private String buildFullName(UserEntity u) {
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last = u.getLastName() == null ? "" : u.getLastName();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? u.getEmail() : full;
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
