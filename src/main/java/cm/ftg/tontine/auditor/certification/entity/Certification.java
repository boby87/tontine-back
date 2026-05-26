package cm.ftg.tontine.auditor.certification.entity;

import cm.ftg.tontine.auditor.certification.enums.CertificationDecision;
import cm.ftg.tontine.auditor.certification.enums.CertificationScope;
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

@Entity
@Table(name = "certifications", indexes = {
        @Index(name = "idx_certification_tontine", columnList = "tontine_id"),
        @Index(name = "idx_certification_issued_at", columnList = "issued_at")
})
@Getter
@Setter
@NoArgsConstructor
public class Certification {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CertificationScope scope;

    @Column(name = "period_label", nullable = false, length = 80)
    private String periodLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CertificationDecision decision;

    @Column(length = 4000)
    private String reserves;

    @Column(name = "issued_by_user_id", nullable = false)
    private UUID issuedByUserId;

    @Column(name = "issued_by_full_name", nullable = false, length = 160)
    private String issuedByFullName;

    @CreationTimestamp
    @Column(name = "issued_at", updatable = false, nullable = false)
    private Instant issuedAt;

    @Version
    private Long version;
}
