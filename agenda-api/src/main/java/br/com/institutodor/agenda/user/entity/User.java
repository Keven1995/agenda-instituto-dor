package br.com.institutodor.agenda.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    private UUID id;
    @Column(name = "google_id", nullable = false, unique = true)
    private String googleId;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String name;
    @Column(name = "refresh_token_encrypted")
    private String refreshTokenEncrypted;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() { }

    public User(UUID id, String googleId, String email, String name) {
        this.id = id;
        this.googleId = googleId;
        this.email = email;
        this.name = name;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public String getGoogleId() { return googleId; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getRefreshTokenEncrypted() { return refreshTokenEncrypted; }
}
