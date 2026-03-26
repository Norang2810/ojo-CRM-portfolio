package org.backend.domain.admin.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "admin",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_admin_email", columnNames = "email")
        }
)
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_id")
    private Long id;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(nullable = false)
    private Boolean google;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Admin(String name, String email, String password, String phone,
                  Boolean google, AdminRole role, AdminStatus status) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.google = google;
        this.role = role;
        this.status = status;
    }

    public static Admin createGoogleUser(String name, String email) {
        return Admin.builder()
                .name(name)
                .email(email)
                .password(null)
                .phone("000-0000-0000")
                .google(true)
                .role(AdminRole.GUEST)
                .status(AdminStatus.ACTIVE)
                .build();
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void changeRole(AdminRole role) {
        this.role = role;
    }

    public void changeStatus(AdminStatus status) {
        this.status = status;
    }
}
