package br.com.institutodor.agenda.notification;

import br.com.institutodor.agenda.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "push_subscriptions")
public class PushSubscription {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false, unique = true, columnDefinition = "TEXT") private String endpoint;
    @Column(nullable = false, columnDefinition = "TEXT") private String p256dh;
    @Column(nullable = false, columnDefinition = "TEXT") private String auth;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected PushSubscription() { }

    public PushSubscription(User user, String endpoint, String p256dh, String auth) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void refreshKeys(String p256dh, String auth) { this.p256dh = p256dh; this.auth = auth; this.updatedAt = Instant.now(); }
    public User getUser() { return user; }
    public String getEndpoint() { return endpoint; }
    public String getP256dh() { return p256dh; }
    public String getAuth() { return auth; }
}
