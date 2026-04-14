package cm.ftg.tontine.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Séance planifiée d'une tontine.
 * CDC Section 7.1 — entité Session.
 * Note : la table est nommée "tontine_session" pour éviter le mot réservé SQL "SESSION".
 */
@Entity
@Table(name = "tontine_session")
public class Session extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tontine_id", nullable = false)
    private Tontine tontine;

    @Column(name = "session_number", nullable = false)
    private int sessionNumber;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time_planned")
    private LocalTime endTimePlanned;

    @Column(name = "location_address")
    private String locationAddress;

    @Column(length = 2000)
    private String agenda;

    /** Bénéficiaire prévu de la cagnotte (mode ROTATION) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id")
    private TontineMember beneficiary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.SCHEDULED;

    /** URL du procès-verbal de réunion (PV) */
    @Column(name = "minutes_url")
    private String minutesUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Tontine getTontine() { return tontine; }
    public void setTontine(Tontine tontine) { this.tontine = tontine; }
    public int getSessionNumber() { return sessionNumber; }
    public void setSessionNumber(int sessionNumber) { this.sessionNumber = sessionNumber; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTimePlanned() { return endTimePlanned; }
    public void setEndTimePlanned(LocalTime endTimePlanned) { this.endTimePlanned = endTimePlanned; }
    public String getLocationAddress() { return locationAddress; }
    public void setLocationAddress(String locationAddress) { this.locationAddress = locationAddress; }
    public String getAgenda() { return agenda; }
    public void setAgenda(String agenda) { this.agenda = agenda; }
    public TontineMember getBeneficiary() { return beneficiary; }
    public void setBeneficiary(TontineMember beneficiary) { this.beneficiary = beneficiary; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    public String getMinutesUrl() { return minutesUrl; }
    public void setMinutesUrl(String minutesUrl) { this.minutesUrl = minutesUrl; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
}
