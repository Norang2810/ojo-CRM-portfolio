package org.backend.domain.analysis.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.backend.domain.member.entity.Member;

import java.time.LocalDate;
import java.time.LocalDateTime;



@Entity
@Getter
@Table(name = "data_usage")
public class DataUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "data_usage_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "usage_time", nullable = false)
    private Integer usageTime; // 0~23

    @Column(name = "usage_amount")
    private Long usageAmount;

    @Column(name = "region", length = 255)
    private String region;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

}