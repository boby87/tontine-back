package cm.ftg.tontine.president.validation.dto;

import cm.ftg.tontine.common.enums.Priority;
import cm.ftg.tontine.president.validation.entity.ValidationItem;
import cm.ftg.tontine.president.validation.enums.DecisionType;
import cm.ftg.tontine.president.validation.enums.DocumentKind;
import cm.ftg.tontine.president.validation.enums.FinancialOperationType;
import cm.ftg.tontine.president.validation.enums.ValidationCategory;
import cm.ftg.tontine.president.validation.enums.ValidationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ValidationDto(
        UUID id,
        UUID tontineId,
        ValidationCategory category,
        ValidationStatus status,
        String title,
        String description,
        BigDecimal amount,
        Priority priority,
        UUID submittedByUserId,
        String submittedByFullName,
        Instant submittedAt,
        AuditorOpinionDto auditorOpinion,
        // financial
        FinancialOperationType operationType,
        String reference,
        UUID borrowerMemberId,
        String borrowerFullName,
        String guarantorsJson,
        String cashBoxName,
        BigDecimal cashBoxBalanceBefore,
        BigDecimal cashBoxBalanceAfter,
        Integer durationMonths,
        BigDecimal interestRate,
        BigDecimal totalDue,
        // document
        DocumentKind documentKind,
        Integer sessionNumber,
        LocalDate sessionDate,
        String location,
        String beneficiary,
        String agendaPointsJson,
        Boolean signedBySecretary,
        Instant signedBySecretaryAt,
        String attachmentsJson,
        String previewSnippet,
        // adhesion
        String candidateFullName,
        String candidatePhone,
        String candidateEmail,
        String sponsorFullName,
        Boolean votedByAssembly,
        String voteResult,
        // decision
        DecisionType decision,
        String decisionComment,
        UUID decidedByUserId,
        Instant decidedAt
) {

    public static ValidationDto from(ValidationItem v) {
        AuditorOpinionDto opinion = v.getAuditorOpinionStatus() != null
                ? new AuditorOpinionDto(v.getAuditorOpinionStatus(), v.getAuditorOpinionComment(),
                v.getAuditorUserId(), v.getAuditorFullName(), v.getAuditorEmittedAt())
                : null;
        return new ValidationDto(
                v.getId(), v.getTontineId(), v.getCategory(), v.getStatus(),
                v.getTitle(), v.getDescription(), v.getAmount(), v.getPriority(),
                v.getSubmittedByUserId(), v.getSubmittedByFullName(), v.getSubmittedAt(), opinion,
                v.getOperationType(), v.getReference(), v.getBorrowerMemberId(), v.getBorrowerFullName(),
                v.getGuarantorsJson(), v.getCashBoxName(), v.getCashBoxBalanceBefore(), v.getCashBoxBalanceAfter(),
                v.getDurationMonths(), v.getInterestRate(), v.getTotalDue(),
                v.getDocumentKind(), v.getSessionNumber(), v.getSessionDate(), v.getLocation(),
                v.getBeneficiary(), v.getAgendaPointsJson(), v.getSignedBySecretary(), v.getSignedBySecretaryAt(),
                v.getAttachmentsJson(), v.getPreviewSnippet(),
                v.getCandidateFullName(), v.getCandidatePhone(), v.getCandidateEmail(),
                v.getSponsorFullName(), v.getVotedByAssembly(), v.getVoteResult(),
                v.getDecision(), v.getDecisionComment(), v.getDecidedByUserId(), v.getDecidedAt());
    }
}
