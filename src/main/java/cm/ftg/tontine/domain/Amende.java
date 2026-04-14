package cm.ftg.tontine.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "amende")
public class Amende {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    private StatutAmende statut = StatutAmende.IMPAYEE;

    @Column(nullable = false)
    private long joursRetard;

    @Column(nullable = false)
    private LocalDateTime dateCalcul;

    public static Amende creer(Membre membre, Seance seance, BigDecimal montant, long joursRetard) {
        var amende = new Amende();
        amende.setMembre(membre);
        amende.setSeance(seance);
        amende.setMontant(montant);
        amende.setJoursRetard(joursRetard);
        amende.setStatut(StatutAmende.IMPAYEE);
        amende.setDateCalcul(LocalDateTime.now());
        return amende;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Membre getMembre() { return membre; }
    public void setMembre(Membre membre) { this.membre = membre; }
    public Seance getSeance() { return seance; }
    public void setSeance(Seance seance) { this.seance = seance; }
    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public StatutAmende getStatut() { return statut; }
    public void setStatut(StatutAmende statut) { this.statut = statut; }
    public long getJoursRetard() { return joursRetard; }
    public void setJoursRetard(long joursRetard) { this.joursRetard = joursRetard; }
    public LocalDateTime getDateCalcul() { return dateCalcul; }
    public void setDateCalcul(LocalDateTime dateCalcul) { this.dateCalcul = dateCalcul; }
}
