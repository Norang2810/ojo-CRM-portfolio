package org.backend.domain.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "refresh_token_sessions",
        uniqueConstraints = @UniqueConstraint(name = "uk_refresh_token_sessions_sid", columnNames = "session_id"),
        indexes = {
                @Index(name = "idx_refresh_token_sessions_admin_id", columnList = "admin_id"),
                @Index(name = "idx_refresh_token_sessions_expires_at", columnList = "expires_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_session_id")
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(name = "refresh_jti", nullable = false, length = 36)
    private String refreshJti;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "fingerprint_hash", nullable = false, length = 64)
    private String fingerprintHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void rotate(String refreshJti, String tokenHash, Instant expiresAt) {
        this.refreshJti = refreshJti;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }
}
