package cm.ftg.tontine.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "membre")
public class Membre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutMembre statut = StatutMembre.ACTIF;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tontine_id", nullable = false)
    private Tontine tontine;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public StatutMembre getStatut() { return statut; }
    public void setStatut(StatutMembre statut) { this.statut = statut; }
    public Tontine getTontine() { return tontine; }
    public void setTontine(Tontine tontine) { this.tontine = tontine; }
}
