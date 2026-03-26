package org.backend.domain.batch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "feature_consultation", uniqueConstraints = {
        @UniqueConstraint(name = "uk_consultation_member_date", columnNames = {"member_id", "feature_base_date"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationBasics {

    // 상담 id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "consultation_id")
    private Long consultationId;

    // 고객 id
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    // 데이터 추출 기준일
    @Column(name = "feature_base_date")
    private LocalDate featureBaseDate;

    // 전체 누적 상담 건수
    @Column(name = "total_consult_count", nullable = false)
    private Integer totalConsultCount;

    // 최근 7일간 상담 건수
    @Column(name = "last_7d_consult_count", nullable = false)
    private Integer last7dConsultCount;

    // 최근 30일간 상담 건수
    @Column(name = "last_30d_consult_count", nullable = false)
    private Integer last30dConsultCount;

    // 월 평균 상담 건수
    @Column(name = "avg_monthly_consult_count", nullable = false)
    private Float avgMonthlyConsultCount;

    // 마지막 상담 날짜
    @Column(name = "last_consult_date")
    private LocalDate lastConsultDate;

    // 야간 상담 건수
    @Column(name = "night_consult_count")
    private Integer nightConsultCount;

    // 주말 or 공휴일 상담 건수
    @Column(name = "weekend_consult_count")
    private Integer weekendConsultCount;

    // 가장 빈번한 상담 카테고리
    @Column(name = "top_consult_category")
    private String topConsultCategory;

    // 누적 불만 상담 건수
    @Column(name = "total_complaint_count", nullable = false)
    private Integer totalComplaintCount;

    // 마지막 상담 이후 경과일
//    @Column(name = "last_consult_days_ago", nullable = false)
    @Column(name = "last_consult_days_ago")
    private Integer lastConsultDaysAgo;
}