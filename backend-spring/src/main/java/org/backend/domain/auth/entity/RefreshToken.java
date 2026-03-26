package org.backend.domain.auth.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "refresh_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_refresh_tokens_admin_id", columnNames = "admin_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_tokens_id")
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "token", nullable = false, length = 1000)
    private String token;


    public void updateToken(String token) {
        this.token = token;
    }
}