package org.backend.domain.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "verification_codes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_target_type", columnNames = {"target", "type"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class VerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_codes_id")
    private Long id;

    @Column(nullable = false)
    private String target; // email or phone

    @Column(nullable = false, length = 30)
    private String type; // EMAIL / PHONE

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false, length = 30)
    private String status; // SENT / VERIFIED / EXPIRED

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
