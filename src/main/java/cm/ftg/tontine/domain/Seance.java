package cm.ftg.tontine.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "seance")
public class Seance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate dateSeance;

    @Column(nullable = false)
    private LocalDate dateLimite;

    @Column(nullable = false)
    private boolean cloturee = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tontine_id", nullable = false)
    private Tontine tontine;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getDateSeance() { return dateSeance; }
    public void setDateSeance(LocalDate dateSeance) { this.dateSeance = dateSeance; }
    public LocalDate getDateLimite() { return dateLimite; }
    public void setDateLimite(LocalDate dateLimite) { this.dateLimite = dateLimite; }
    public boolean isCloturee() { return cloturee; }
    public void setCloturee(boolean cloturee) { this.cloturee = cloturee; }
    public Tontine getTontine() { return tontine; }
    public void setTontine(Tontine tontine) { this.tontine = tontine; }
}
