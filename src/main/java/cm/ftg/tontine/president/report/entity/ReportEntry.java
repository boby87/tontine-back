package cm.ftg.tontine.president.report.entity;

import cm.ftg.tontine.president.report.enums.ReportCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "report_entries", indexes = {
        @Index(name = "idx_report_tontine", columnList = "tontine_id"),
        @Index(name = "idx_report_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
public class ReportEntry {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tontine_id", nullable = false)
    private UUID tontineId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "period_label", nullable = false, length = 80)
    private String periodLabel;

    @Column(name = "author_full_name", nullable = false, length = 160)
    private String authorFullName;

    @CreationTimestamp
    @Column(name = "generated_at", updatable = false, nullable = false)
    private Instant generatedAt;

    @Column(name = "metrics_json", length = 4000)
    private String metricsJson;

    @Column(name = "download_url_pdf", length = 500)
    private String downloadUrlPdf;

    @Column(name = "download_url_excel", length = 500)
    private String downloadUrlExcel;
}
