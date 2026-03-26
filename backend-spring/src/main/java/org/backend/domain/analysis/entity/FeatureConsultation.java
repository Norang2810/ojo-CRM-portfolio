package org.backend.domain.analysis.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.backend.domain.member.entity.Member;

import java.time.LocalDate;

@Entity
@Table(name = "feature_consultation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeatureConsultation {

    @Id
    @Column(name = "consultation_id")
    private Long id;   // 상담 ID (PK)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "feature_base_date")
    private LocalDate featureBaseDate;   // 데이터 추출 기준일

    @Column(name = "total_consult_count", nullable = false)
    private Integer totalConsultCount;   // 전체 누적 상담 건수

    @Column(name = "last_7d_consult_count", nullable = false)
    private Integer last7dConsultCount;  // 최근 7일 상담 건수

    @Column(name = "last_30d_consult_count", nullable = false)
    private Integer last30dConsultCount; // 최근 30일 상담 건수

    @Column(name = "avg_monthly_consult_count", nullable = false)
    private Float avgMonthlyConsultCount; // 월평균 상담 건수

    @Column(name = "last_consult_date")
    private LocalDate lastConsultDate;   // 마지막 상담 날짜

    @Column(name = "night_consult_count")
    private Integer nightConsultCount;   // 야간 상담 건수 (21시~08시)

    @Column(name = "weekend_consult_count")
    private Integer weekendConsultCount; // 주말/공휴일 상담 건수

    @Column(name = "top_consult_category", length = 30)
    private String topConsultCategory;   // 가장 빈번한 상담 카테고리 코드

    @Column(name = "total_complaint_count", nullable = false)
    private Integer totalComplaintCount; // 누적 불만 상담 건수

    @Column(name = "last_consult_days_ago", nullable = false)
    private Integer lastConsultDaysAgo;  // 마지막 상담 이후 경과일

    // 상담 빈도수 계산 (월 평균 1.6 기준)
    private String calculateFrequency(Double count) {

        if (count == null) {
            return "LOW";
        }

        double avg = 1.6;

        // 평균의 약 2배 이상이면 활동이 매우 많음
        if (count >= avg * 2) {
            return "HIGH";
        }

        // 평균 이상이면 보통 수준
        if (count >= avg) {
            return "MEDIUM";
        }

        // 평균 미만이면 낮음
        return "LOW";
    }
}
