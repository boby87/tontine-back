package cm.ftg.tontine.president.vote.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vote_options", indexes = {
        @Index(name = "idx_vote_option_vote", columnList = "vote_id")
})
@Getter
@Setter
@NoArgsConstructor
public class VoteOption {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "vote_id", nullable = false)
    private UUID voteId;

    @Column(nullable = false, length = 300)
    private String label;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private long count = 0;
}
