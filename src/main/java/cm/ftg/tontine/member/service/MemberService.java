package cm.ftg.tontine.member.service;

import cm.ftg.tontine.auth.entity.UserEntity;
import cm.ftg.tontine.auth.repository.UserRepository;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.member.dto.MemberDto;
import cm.ftg.tontine.member.dto.MemberSummaryDto;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.tontine.dto.TontineDto;
import cm.ftg.tontine.tontine.entity.Tontine;
import cm.ftg.tontine.tontine.repository.TontineRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final TontineRepository tontineRepository;
    private final UserRepository userRepository;

    public MemberService(MemberRepository memberRepository,
                         TontineRepository tontineRepository,
                         UserRepository userRepository) {
        this.memberRepository = memberRepository;
        this.tontineRepository = tontineRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public MemberSummaryDto getMySummary(UUID userId, UUID tontineIdHint) {
        Member member = resolveActiveMember(userId, tontineIdHint);
        Tontine tontine = tontineRepository.findById(member.getTontineId()).orElse(null);
        TontineDto tontineDto = tontine != null ? TontineDto.from(tontine) : null;
        return new MemberSummaryDto(
                MemberDto.from(member),
                tontineDto,
                member.getTotalContributed(),
                member.getTotalArrears(),
                0,                  // activeLoans — module Prets non encore implemente
                null,               // nextSession — module Sessions non encore implemente
                member.getTourOrder(),
                member.getTontineId());
    }

    @Transactional(readOnly = true)
    public List<MemberDto> listMyMemberships(UUID userId) {
        return memberRepository.findByUserId(userId).stream()
                .map(MemberDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Member resolveActiveMember(UUID userId, UUID tontineIdHint) {
        UUID effective = tontineIdHint;
        if (effective == null) {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException("AUTH_REQUIRED",
                            "Session invalide", HttpStatus.UNAUTHORIZED));
            effective = user.getActiveTontineId();
        }
        Optional<Member> member;
        if (effective != null) {
            member = memberRepository.findByUserIdAndTontineId(userId, effective);
        } else {
            member = memberRepository.findByUserId(userId).stream().findFirst();
        }
        return member.orElseThrow(() -> new ApiException("MEMBER_NO_MEMBERSHIP",
                "Aucune adhesion active trouvee. Selectionnez une tontine via X-Tontine-Id.",
                HttpStatus.NOT_FOUND));
    }
}
