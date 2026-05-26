package cm.ftg.tontine.secretary.registry.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.secretary.registry.dto.MemberRegistryDto;
import cm.ftg.tontine.secretary.registry.dto.UpdateMemberRegistryRequest;
import cm.ftg.tontine.secretary.security.SecretaryAccessChecker;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberRegistryService {

    private final MemberRepository memberRepository;
    private final SecretaryAccessChecker accessChecker;
    private final AuditService auditService;

    public MemberRegistryService(MemberRepository memberRepository,
                                 SecretaryAccessChecker accessChecker,
                                 AuditService auditService) {
        this.memberRepository = memberRepository;
        this.accessChecker = accessChecker;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<MemberRegistryDto> list(UUID tontineId, UUID userId) {
        accessChecker.requireSecretary(userId, tontineId);
        return memberRepository.findByTontineIdOrderByMatriculeAsc(tontineId).stream()
                .map(MemberRegistryDto::from)
                .toList();
    }

    @Transactional
    public MemberRegistryDto update(UUID id, UUID tontineId, UUID userId,
                                    UpdateMemberRegistryRequest req) {
        accessChecker.requireSecretary(userId, tontineId);
        Member m = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member", id));
        if (!m.getTontineId().equals(tontineId)) {
            throw new ApiException("FORBIDDEN", "Membre hors tontine courante", HttpStatus.FORBIDDEN);
        }
        StringBuilder changes = new StringBuilder("{");
        boolean first = true;
        if (req.phone() != null && !Objects.equals(req.phone(), m.getPhone())) {
            changes.append("\"phone\":\"changed\"");
            first = false;
            m.setPhone(req.phone());
        }
        if (req.email() != null && !Objects.equals(req.email(), m.getEmail())) {
            if (!first) changes.append(",");
            changes.append("\"email\":\"changed\"");
            first = false;
            m.setEmail(req.email());
        }
        if (req.matricule() != null && !req.matricule().isBlank()
                && !Objects.equals(req.matricule(), m.getMatricule())) {
            if (!first) changes.append(",");
            changes.append("\"matricule\":\"changed\"");
            m.setMatricule(req.matricule());
        }
        changes.append("}");
        Member saved = memberRepository.save(m);
        auditService.record(userId, "MEMBER_REGISTRY_UPDATE", "Member", id.toString(),
                tontineId, changes.toString());
        return MemberRegistryDto.from(saved);
    }
}
