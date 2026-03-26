package org.backend.domain.advice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.backend.domain.admin.entity.Admin;
import org.backend.domain.member.entity.Member;
import org.backend.domain.subscription.entity.Promotion;

import java.time.LocalDateTime;


@Entity
@Table(name = "advice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Advice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "advice_id")
    private Long id;

    // 상담을 받은 고객
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 상담을 진행한 관리자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    // 상담 카테고리
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Categories category;

    // 특정 프로모션 관련 상담인 경우
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    // IN / OUT bound 구분
    @Column(nullable = false, length = 255)
    private String direction;

    // CALL, APP, SMS 등 구분
    @Column(nullable = false, length = 255)
    private String channel;

    @Column(name = "advice_content", columnDefinition = "TEXT")
    private String adviceContent;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "satisfaction_score")
    private Long satisfactionScore;

}