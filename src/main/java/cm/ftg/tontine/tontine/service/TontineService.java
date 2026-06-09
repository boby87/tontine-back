package cm.ftg.tontine.tontine.service;

import cm.ftg.tontine.audit.service.AuditService;
import cm.ftg.tontine.auth.entity.UserEntity;
import cm.ftg.tontine.auth.repository.UserRepository;
import cm.ftg.tontine.common.enums.MemberStatus;
import cm.ftg.tontine.common.enums.TontineStatus;
import cm.ftg.tontine.common.enums.UserRole;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.common.exception.ResourceNotFoundException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.president.membership.invitation.enums.InvitationChannel;
import cm.ftg.tontine.president.membership.invitation.service.InvitationService;
import cm.ftg.tontine.tontine.dto.CreateTontineRequest;
import cm.ftg.tontine.tontine.dto.CycleDto;
import cm.ftg.tontine.tontine.dto.FounderInviteDto;
import cm.ftg.tontine.tontine.dto.TontineDto;
import cm.ftg.tontine.tontine.dto.UpdateTontineRequest;
import cm.ftg.tontine.tontine.entity.Tontine;
import cm.ftg.tontine.tontine.repository.CycleRepository;
import cm.ftg.tontine.tontine.repository.TontineRepository;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TontineService {

    private final TontineRepository tontineRepository;
    private final CycleRepository cycleRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final InvitationService invitationService;

    public TontineService(TontineRepository tontineRepository,
                          CycleRepository cycleRepository,
                          MemberRepository memberRepository,
                          UserRepository userRepository,
                          AuditService auditService,
                          InvitationService invitationService) {
        this.tontineRepository = tontineRepository;
        this.cycleRepository = cycleRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.invitationService = invitationService;
    }

    @Transactional(readOnly = true)
    public Page<TontineDto> list(String search, Pageable pageable) {
        return tontineRepository.search(search, pageable).map(TontineDto::from);
    }

    @Transactional(readOnly = true)
    public List<TontineDto> listMine(UUID userId) {
        List<UUID> tontineIds = memberRepository.findByUserId(userId).stream()
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE || m.getStatus() == MemberStatus.PENDING)
                .map(Member::getTontineId)
                .toList();
        if (tontineIds.isEmpty()) {
            return List.of();
        }
        return tontineRepository.findAllById(tontineIds).stream()
                .map(TontineDto::from)
                .toList();
    }

    @Transactional
    public TontineDto create(UUID creatorUserId, CreateTontineRequest req) {
        UserEntity creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", creatorUserId));

        Tontine tontine = new Tontine();
        tontine.setName(req.name().trim());
        tontine.setDescription(req.description());
        tontine.setStatus(TontineStatus.DRAFT);
        tontine.setContributionAmount(req.contributionAmount());
        tontine.setFrequency(req.frequency());
        tontine.setStartDate(req.startDate());
        tontine.setMaxMembers(req.maxMembers());
        tontine.setCreatedByUserId(creatorUserId);
        tontine.setRules(req.rules().toEmbeddable());
        tontine.setTotalSaved(BigDecimal.ZERO);
        req.founders().forEach(f -> tontine.getFounders().add(f.toEmbeddable()));
        Tontine saved = tontineRepository.save(tontine);

        Member creatorMember = buildCreatorMember(creator, saved);
        memberRepository.save(creatorMember);
        saved.setMemberCount(1);

        // Auto-invitation des fondateurs (hors createur) : c'est l'acceptation de
        // l'invitation qui creera le Member ACTIVE. Le createur reste l'unique President.
        for (FounderInviteDto f : req.founders()) {
            if (isCreator(f, creator)) {
                continue;
            }
            if (f.role() == UserRole.PRESIDENT) {
                throw new ApiException("FOUNDER_PRESIDENT_FORBIDDEN",
                        "Un fondateur ne peut pas etre declare President", HttpStatus.valueOf(422));
            }
            Set<InvitationChannel> channels = EnumSet.of(InvitationChannel.SMS);
            if (f.email() != null && !f.email().isBlank()) {
                channels.add(InvitationChannel.EMAIL);
            }
            invitationService.inviteFromFounder(saved.getId(), creatorUserId, creator, f, channels);
        }
        saved.setMemberCount((int) memberRepository.countByTontineId(saved.getId()));
        tontineRepository.save(saved);

        if (creator.getActiveTontineId() == null) {
            creator.setActiveTontineId(saved.getId());
            userRepository.save(creator);
        }

        auditService.record(creatorUserId, "TONTINE_CREATE", "Tontine", saved.getId().toString(),
                saved.getId(), "{\"name\":\"" + saved.getName() + "\"}");
        return TontineDto.from(saved);
    }

    @Transactional(readOnly = true)
    public TontineDto findById(UUID id, UUID requesterUserId, Set<UserRole> requesterRoles) {
        Tontine tontine = tontineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tontine", id));
        assertCanRead(tontine, requesterUserId, requesterRoles);
        return TontineDto.from(tontine);
    }

    @Transactional
    public TontineDto update(UUID id, UpdateTontineRequest req, UUID requesterUserId) {
        Tontine tontine = tontineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tontine", id));
        assertIsPresident(tontine.getId(), requesterUserId);
        if (req.name() != null) {
            tontine.setName(req.name().trim());
        }
        if (req.description() != null) {
            tontine.setDescription(req.description());
        }
        if (req.status() != null) {
            tontine.setStatus(req.status());
        }
        if (req.contributionAmount() != null) {
            tontine.setContributionAmount(req.contributionAmount());
        }
        if (req.frequency() != null) {
            tontine.setFrequency(req.frequency());
        }
        if (req.maxMembers() != null) {
            if (req.maxMembers() < tontine.getMemberCount()) {
                throw new ApiException("TONTINE_INVALID_MAX_MEMBERS",
                        "maxMembers ne peut pas etre inferieur au nombre de membres actuels",
                        HttpStatus.valueOf(422));
            }
            tontine.setMaxMembers(req.maxMembers());
        }
        if (req.startDate() != null) {
            tontine.setStartDate(req.startDate());
        }
        if (req.endDate() != null) {
            tontine.setEndDate(req.endDate());
        }
        if (req.rules() != null) {
            tontine.setRules(req.rules().toEmbeddable());
        }
        Tontine saved = tontineRepository.save(tontine);
        auditService.record(requesterUserId, "TONTINE_UPDATE", "Tontine", saved.getId().toString(),
                saved.getId(), null);
        return TontineDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CycleDto> listCycles(UUID tontineId, UUID requesterUserId, Set<UserRole> requesterRoles) {
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new ResourceNotFoundException("Tontine", tontineId));
        assertCanRead(tontine, requesterUserId, requesterRoles);
        return cycleRepository.findByTontineIdOrderByNumberAsc(tontineId).stream()
                .map(CycleDto::from)
                .toList();
    }

    private Member buildCreatorMember(UserEntity creator, Tontine tontine) {
        Member m = new Member();
        m.setUserId(creator.getId());
        m.setTontineId(tontine.getId());
        m.setMatricule("M-001");
        m.setFirstName(creator.getFirstName());
        m.setLastName(creator.getLastName());
        m.setPhone(creator.getPhone());
        m.setEmail(creator.getEmail());
        m.setStatus(MemberStatus.ACTIVE);
        m.setRoles(EnumSet.of(UserRole.PRESIDENT, UserRole.MEMBER));
        return m;
    }

    private boolean isCreator(FounderInviteDto f, UserEntity creator) {
        return f.phone() != null && f.phone().equalsIgnoreCase(creator.getPhone());
    }

    private void assertCanRead(Tontine tontine, UUID userId, Set<UserRole> roles) {
        if (roles != null && roles.contains(UserRole.ADMIN)) {
            return;
        }
        boolean isMember = memberRepository.findByUserIdAndTontineId(userId, tontine.getId()).isPresent();
        if (!isMember && !tontine.getCreatedByUserId().equals(userId)) {
            throw new ApiException("FORBIDDEN",
                    "Vous n'avez pas acces a cette tontine", HttpStatus.FORBIDDEN);
        }
    }

    private void assertIsPresident(UUID tontineId, UUID userId) {
        Member m = memberRepository.findByUserIdAndTontineId(userId, tontineId)
                .orElseThrow(() -> new ApiException("FORBIDDEN",
                        "Vous n'etes pas membre de cette tontine", HttpStatus.FORBIDDEN));
        if (!m.getRoles().contains(UserRole.PRESIDENT)) {
            throw new ApiException("FORBIDDEN",
                    "Seul le President peut effectuer cette action", HttpStatus.FORBIDDEN);
        }
    }
}
