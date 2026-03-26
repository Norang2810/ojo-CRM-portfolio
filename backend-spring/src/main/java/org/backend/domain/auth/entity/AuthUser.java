package org.backend.domain.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_users_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 100)
    private String name;

    @Column(nullable = false, length = 30)
    private String provider; // GOOGLE

    @Column(nullable = false, length = 30)
    private String role; // ROLE_ADMIN / ROLE_AGENT

    @Column(nullable = false, length = 30)
    private String status; // ACTIVE / BLOCKED

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
