package br.com.institutodor.agenda.appointment.entity;

import br.com.institutodor.agenda.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "appointments")
public class Appointment {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "patient_name", nullable = false)
    private String patientName;
    @Column(name = "start_at", nullable = false)
    private Instant startAt;
    @Column(name = "end_at", nullable = false)
    private Instant endAt;
    @Column(name = "google_event_id")
    private String googleEventId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AppointmentStatus status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "notification_sent_at") private Instant notificationSentAt;

    protected Appointment() { }

    public static Appointment scheduled(User user, String patientName, Instant startAt, Instant endAt) {
        var appointment = new Appointment();
        appointment.id = UUID.randomUUID();
        appointment.user = user;
        appointment.patientName = patientName;
        appointment.startAt = startAt;
        appointment.endAt = endAt;
        appointment.status = AppointmentStatus.SCHEDULED;
        appointment.createdAt = Instant.now();
        appointment.updatedAt = appointment.createdAt;
        return appointment;
    }

    public void update(String patientName, Instant startAt, Instant endAt) {
        this.patientName = patientName;
        this.startAt = startAt;
        this.endAt = endAt;
        this.updatedAt = Instant.now();
    }

    public void cancel() { this.status = AppointmentStatus.CANCELLED; this.updatedAt = Instant.now(); }
    public void setGoogleEventId(String googleEventId) { this.googleEventId = googleEventId; this.updatedAt = Instant.now(); }
    public void markNotificationSent() { this.notificationSentAt = Instant.now(); this.updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getPatientName() { return patientName; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public String getGoogleEventId() { return googleEventId; }
    public AppointmentStatus getStatus() { return status; }
    public Instant getNotificationSentAt() { return notificationSentAt; }
}
