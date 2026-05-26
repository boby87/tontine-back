package cm.ftg.tontine.president.membership.dto;

import cm.ftg.tontine.president.membership.entity.MembershipFile;
import cm.ftg.tontine.president.membership.enums.MembershipFileKind;
import cm.ftg.tontine.president.membership.enums.MembershipFileStatus;
import java.time.Instant;
import java.util.UUID;

public record MembershipFileDto(
        UUID id,
        UUID tontineId,
        MembershipFileKind kind,
        String candidateFullName,
        String candidatePhone,
        String candidateEmail,
        UUID memberId,
        String sponsorFullName,
        String motivation,
        MembershipFileStatus status,
        Instant submittedAt,
        Instant bureauReviewedAt,
        Instant assemblyVotedAt,
        Integer assemblyVoteYes,
        Integer assemblyVoteNo,
        Integer assemblyVoteAbstain,
        Instant presidentDecidedAt,
        String presidentDecisionComment
) {

    public static MembershipFileDto from(MembershipFile m) {
        return new MembershipFileDto(
                m.getId(), m.getTontineId(), m.getKind(),
                m.getCandidateFullName(), m.getCandidatePhone(), m.getCandidateEmail(),
                m.getMemberId(), m.getSponsorFullName(), m.getMotivation(), m.getStatus(),
                m.getSubmittedAt(), m.getBureauReviewedAt(), m.getAssemblyVotedAt(),
                m.getAssemblyVoteYes(), m.getAssemblyVoteNo(), m.getAssemblyVoteAbstain(),
                m.getPresidentDecidedAt(), m.getPresidentDecisionComment());
    }
}
