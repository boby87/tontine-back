package cm.ftg.tontine.president.security;

import cm.ftg.tontine.common.enums.UserRole;
import cm.ftg.tontine.common.exception.ApiException;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PresidentAccessChecker {

    private final MemberRepository memberRepository;

    public PresidentAccessChecker(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member requirePresident(UUID userId, UUID tontineId) {
        Member m = memberRepository.findByUserIdAndTontineId(userId, tontineId)
                .orElseThrow(() -> new ApiException("FORBIDDEN",
                        "Non membre de cette tontine", HttpStatus.FORBIDDEN));
        if (!m.getRoles().contains(UserRole.PRESIDENT)) {
            throw new ApiException("FORBIDDEN",
                    "Action reservee au President", HttpStatus.FORBIDDEN);
        }
        return m;
    }

    public Member requireMember(UUID userId, UUID tontineId) {
        return memberRepository.findByUserIdAndTontineId(userId, tontineId)
                .orElseThrow(() -> new ApiException("FORBIDDEN",
                        "Non membre de cette tontine", HttpStatus.FORBIDDEN));
    }
}
