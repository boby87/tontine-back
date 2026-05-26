package cm.ftg.tontine.president.delegation.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auth.entity.UserEntity;
import cm.ftg.tontine.auth.repository.UserRepository;
import cm.ftg.tontine.common.enums.UserRole;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.president.delegation.dto.CreateDelegationRequest;
import cm.ftg.tontine.president.delegation.dto.DelegationDto;
import cm.ftg.tontine.president.delegation.dto.RevokeDelegationRequest;
import cm.ftg.tontine.president.delegation.entity.Delegation;
import cm.ftg.tontine.president.delegation.enums.DelegationStatus;
import cm.ftg.tontine.president.delegation.repository.DelegationRepository;
import cm.ftg.tontine.president.security.PresidentAccessChecker;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DelegationService {

    private final DelegationRepository repository;
    private final PresidentAccessChecker accessChecker;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public DelegationService(DelegationRepository repository,
                             PresidentAccessChecker accessChecker,
                             AuditService auditService,
                             UserRepository userRepository) {
        this.repository = repository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<DelegationDto> list(UUID tontineId, UUID userId) {
        accessChecker.requirePresident(userId, tontineId);
        return repository.findByTontineIdOrderByCreatedAtDesc(tontineId).stream()
                .map(DelegationDto::from)
                .toList();
    }

    @Transactional
    public DelegationDto create(UUID tontineId, UUID userId, CreateDelegationRequest req) {
        accessChecker.requirePresident(userId, tontineId);
        if (!req.startsAt().isBefore(req.endsAt())) {
            throw new ApiException("DELEGATION_INVALID_WINDOW",
                    "startsAt doit etre anterieur a endsAt", HttpStatus.valueOf(422));
        }
        UserEntity delegatee = userRepository.findById(req.delegateeUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.delegateeUserId()));
        Delegation d = new Delegation();
        d.setTontineId(tontineId);
        d.setDelegateeUserId(delegatee.getId());
        d.setDelegateeFullName(buildFullName(delegatee));
        d.setDelegateeRole(pickPrimaryRole(delegatee.getRoles()));
        d.setPowers(EnumSet.copyOf(req.powers()));
        d.setReason(req.reason());
        d.setStartsAt(req.startsAt());
        d.setEndsAt(req.endsAt());
        d.setStatus(DelegationStatus.ACTIVE);
        Delegation saved = repository.save(d);
        auditService.record(userId, "DELEGATION_CREATE", "Delegation", saved.getId().toString(), tontineId,
                "{\"delegateeUserId\":\"" + delegatee.getId() + "\"}");
        return DelegationDto.from(saved);
    }

    @Transactional
    public DelegationDto revoke(UUID id, UUID tontineId, UUID userId, RevokeDelegationRequest req) {
        accessChecker.requirePresident(userId, tontineId);
        Delegation d = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delegation", id));
        if (!d.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Tontine non concordante", HttpStatus.FORBIDDEN);
        }
        if (d.getStatus() != DelegationStatus.ACTIVE) {
            throw new ApiException("DELEGATION_INVALID_STATE",
                    "Seule une delegation active peut etre revoquee", HttpStatus.CONFLICT);
        }
        d.setStatus(DelegationStatus.REVOKED);
        d.setRevokedAt(Instant.now());
        d.setRevokedReason(req.reason());
        Delegation saved = repository.save(d);
        auditService.record(userId, "DELEGATION_REVOKE", "Delegation", id.toString(), tontineId,
                "{\"reason\":\"" + escape(req.reason()) + "\"}");
        return DelegationDto.from(saved);
    }

    private UserRole pickPrimaryRole(Set<UserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return UserRole.MEMBER;
        }
        for (UserRole r : List.of(UserRole.PRESIDENT, UserRole.SECRETARY, UserRole.TREASURER,
                UserRole.CENSOR, UserRole.AUDITOR, UserRole.MEMBER)) {
            if (roles.contains(r)) {
                return r;
            }
        }
        return UserRole.MEMBER;
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
