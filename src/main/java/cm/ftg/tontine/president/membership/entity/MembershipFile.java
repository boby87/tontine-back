package cm.ftg.tontine.president.membership.entity;

import cm.ftg.tontine.president.membership.enums.MembershipFileKind;
import cm.ftg.tontine.president.membership.enums.MembershipFileStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

// TODO: attachments and history collections not yet modelled

@Entity
@Table(name = "membership_files", indexes = {
        @Index(name = "idx_membership_tontine", columnList = "tontine_id"),
        @Index(name = "idx_membership_kind", columnList = "kind"),
        @Index(name = "idx_membership_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class MembershipFile {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipFileKind kind;

    @Column(name = "candidate_full_name", nullable = false, length = 160)
    private String candidateFullName;

    @Column(name = "candidate_phone", length = 20)
    private String candidatePhone;

    @Column(name = "candidate_email", length = 160)
    private String candidateEmail;

    @Column(name = "member_id")
    private UUID memberId;

    @Column(name = "sponsor_full_name", length = 160)
    private String sponsorFullName;

    @Column(nullable = false, length = 2000)
    private String motivation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MembershipFileStatus status = MembershipFileStatus.SUBMITTED;

    @CreationTimestamp
    @Column(name = "submitted_at", updatable = false, nullable = false)
    private Instant submittedAt;

    @Column(name = "bureau_reviewed_at")
    private Instant bureauReviewedAt;

    @Column(name = "assembly_voted_at")
    private Instant assemblyVotedAt;

    @Column(name = "assembly_vote_yes")
    private Integer assemblyVoteYes;

    @Column(name = "assembly_vote_no")
    private Integer assemblyVoteNo;

    @Column(name = "assembly_vote_abstain")
    private Integer assemblyVoteAbstain;

    @Column(name = "president_decided_at")
    private Instant presidentDecidedAt;

    @Column(name = "president_decision_comment", length = 2000)
    private String presidentDecisionComment;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
