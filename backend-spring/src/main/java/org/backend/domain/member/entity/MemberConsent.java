package org.backend.domain.member.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "member_consent")
public class MemberConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_consent_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(name = "personal_accepted", nullable = false, length = 1)
    private String personalAccepted;

    @Column(name = "marketing_accepted", nullable = false, length = 1)
    private String marketingAccepted;

    @Column(name = "is_converted", length = 1)
    private String isConverted;

    @Column(name = "accepted_at", nullable = false)
    private LocalDateTime acceptedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

}
