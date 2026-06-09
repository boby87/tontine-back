package cm.ftg.tontine.member.vote.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
        name = "vote_ballots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_vote_ballot_member",
                columnNames = {"vote_id", "voter_key"}
        ),
        indexes = {
                @Index(name = "idx_vote_ballot_vote", columnList = "vote_id"),
                @Index(name = "idx_vote_ballot_option", columnList = "option_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class VoteBallot {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "vote_id", nullable = false)
    private UUID voteId;

    @Column(name = "option_id", nullable = false)
    private UUID optionId;

    /**
     * Cle d'identification du votant :
     *  - non-anonyme : memberId.toString()
     *  - anonyme : SHA-256(voteId + memberId + serverSecret) en hex
     * La contrainte UNIQUE (vote_id, voter_key) garantit l'unicite du vote.
     */
    @Column(name = "voter_key", nullable = false, length = 80)
    private String voterKey;

    /** Renseigne uniquement si Vote.isAnonymous = false, sinon NULL. */
    @Column(name = "member_id")
    private UUID memberId;

    @CreationTimestamp
    @Column(name = "cast_at", updatable = false, nullable = false)
    private Instant castAt;
}
