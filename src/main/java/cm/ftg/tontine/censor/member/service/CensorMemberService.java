package cm.ftg.tontine.censor.member.service;

import cm.ftg.tontine.censor.member.dto.CensorMemberDto;
import cm.ftg.tontine.censor.security.CensorAccessChecker;
import cm.ftg.tontine.common.enums.MemberStatus;
import cm.ftg.tontine.member.repository.MemberRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CensorMemberService {

    private final MemberRepository memberRepository;
    private final CensorAccessChecker accessChecker;

    public CensorMemberService(MemberRepository memberRepository, CensorAccessChecker accessChecker) {
        this.memberRepository = memberRepository;
        this.accessChecker = accessChecker;
    }

    @Transactional(readOnly = true)
    public List<CensorMemberDto> listActive(UUID tontineId, UUID userId) {
        accessChecker.requireCensor(userId, tontineId);
        return memberRepository.findByTontineIdOrderByMatriculeAsc(tontineId).stream()
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .map(CensorMemberDto::from)
                .toList();
    }
}
