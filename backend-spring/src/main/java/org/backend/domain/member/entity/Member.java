package org.backend.domain.member.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "member",
        indexes = {
                @Index(name = "idx_member_created_status", columnList = "created_at, status"),
                @Index(name = "idx_member_updated_member", columnList = "updated_at, member_id")
        }
)
@Getter
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(nullable = false, length = 50)
    private String email;

    @Column(nullable = false, length = 10)
    private String gender;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 255)
    private String region;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(name = "household_type", nullable = false)
    private Integer householdType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false,
            columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)")
    private LocalDateTime updatedAt;

    @Column(nullable = false, length = 20)
    private String status;

    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY)
    private MemberConsent consent;

}
