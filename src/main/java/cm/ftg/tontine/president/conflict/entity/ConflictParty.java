package cm.ftg.tontine.president.conflict.entity;

import cm.ftg.tontine.president.conflict.enums.ConflictPartyRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conflict_parties", indexes = {
        @Index(name = "idx_conflict_party_conflict", columnList = "conflict_id"),
        @Index(name = "idx_conflict_party_member", columnList = "member_id")
})
@Getter
@Setter
@NoArgsConstructor
public class ConflictParty {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "conflict_id", nullable = false)
    private UUID conflictId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ConflictPartyRole role;
}
