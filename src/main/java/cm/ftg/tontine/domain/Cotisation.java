package cm.ftg.tontine.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cotisation", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"membre_id", "seance_id"})
})
public class Cotisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String cleIdempotence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seance_id", nullable = false)
    private Seance seance;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCotisation statut;

    @Column(nullable = false)
    private LocalDateTime dateOperation;

    @Version
    private Long version;

    public static Cotisation creer(Membre membre, Seance seance, BigDecimal montant,
                                    StatutCotisation statut, String cleIdempotence) {
        var cotisation = new Cotisation();
        cotisation.setMembre(membre);
        cotisation.setSeance(seance);
        cotisation.setMontant(montant);
        cotisation.setStatut(statut);
        cotisation.setCleIdempotence(cleIdempotence);
        cotisation.setDateOperation(LocalDateTime.now());
        return cotisation;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCleIdempotence() { return cleIdempotence; }
    public void setCleIdempotence(String cleIdempotence) { this.cleIdempotence = cleIdempotence; }
    public Membre getMembre() { return membre; }
    public void setMembre(Membre membre) { this.membre = membre; }
    public Seance getSeance() { return seance; }
    public void setSeance(Seance seance) { this.seance = seance; }
    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public StatutCotisation getStatut() { return statut; }
    public void setStatut(StatutCotisation statut) { this.statut = statut; }
    public LocalDateTime getDateOperation() { return dateOperation; }
    public void setDateOperation(LocalDateTime dateOperation) { this.dateOperation = dateOperation; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
