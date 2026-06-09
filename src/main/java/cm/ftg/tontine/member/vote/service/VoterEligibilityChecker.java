package cm.ftg.tontine.member.vote.service;

import cm.ftg.tontine.common.enums.MemberStatus;
import cm.ftg.tontine.common.enums.UserRole;
import cm.ftg.tontine.member.entity.Member;
import cm.ftg.tontine.member.repository.MemberRepository;
import cm.ftg.tontine.president.vote.entity.Vote;
import cm.ftg.tontine.president.vote.enums.VoteAudience;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Determine si un membre est eligible a voir/voter une motion selon le champ
 * {@link Vote#getAudience()}. Regles definies dans le brief (Section 3.1).
 */
@Component
public class VoterEligibilityChecker {

    private static final Set<UserRole> BUREAU_ROLES = EnumSet.of(
            UserRole.PRESIDENT, UserRole.SECRETARY, UserRole.TREASURER,
            UserRole.CENSOR, UserRole.AUDITOR);

    private final MemberRepository memberRepository;

    public VoterEligibilityChecker(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /** Vrai si le membre (rattache a la tontine) satisfait l'audience du vote. */
    public boolean isEligible(UUID userId, UUID tontineId, Vote vote) {
        return memberRepository.findByUserIdAndTontineId(userId, tontineId)
                .map(m -> matchesAudience(m, vote.getAudience()))
                .orElse(false);
    }

    /** Nombre de membres eligibles a l'audience du vote — sert au calcul du quorum. */
    public int countEligible(Vote vote) {
        return (int) memberRepository.findByTontineId(vote.getTontineId()).stream()
                .filter(m -> matchesAudience(m, vote.getAudience()))
                .count();
    }

    private boolean matchesAudience(Member member, VoteAudience audience) {
        return switch (audience) {
            case ALL -> true;
            case BUREAU -> member.getRoles().stream().anyMatch(BUREAU_ROLES::contains);
            case MEMBERS_ACTIVE -> member.getStatus() == MemberStatus.ACTIVE;
        };
    }
}
