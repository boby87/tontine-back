package cm.ftg.tontine.treasurer.security;

import cm.ftg.tontine.common.enums.UserRole;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class TreasurerAccessChecker {

    private final MemberRepository memberRepository;

    public TreasurerAccessChecker(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member requireTreasurer(UUID userId, UUID tontineId) {
        Member m = memberRepository.findByUserIdAndTontineId(userId, tontineId)
                .orElseThrow(() -> new ApiException("FORBIDDEN",
                        "Non membre de cette tontine", HttpStatus.FORBIDDEN));
        if (!m.getRoles().contains(UserRole.TREASURER) && !m.getRoles().contains(UserRole.PRESIDENT)) {
            throw new ApiException("FORBIDDEN",
                    "Action reservee au Tresorier (ou au President)", HttpStatus.FORBIDDEN);
        }
        return m;
    }
}
