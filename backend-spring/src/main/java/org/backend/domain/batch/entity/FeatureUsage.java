package org.backend.domain.batch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "feature_usage", uniqueConstraints = {
        @UniqueConstraint(name = "uk_usage_member_date", columnNames = {"member_id", "feature_base_date"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usage_id")
    private Long usageId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "feature_base_date")
    private LocalDate featureBaseDate;

    @Column(name = "total_usage_amount", nullable = false)
    private Long totalUsageAmount;

    @Column(name = "avg_daily_usage", nullable = false)
    private Float avgDailyUsage;

    @Column(name = "max_usage_amount", nullable = false)
    private Long maxUsageAmount;

    @Column(name = "usage_peak_hour")
    private Integer usagePeakHour;

    @Column(name = "premium_service_count", nullable = false)
    private Integer premiumServiceCount;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @Column(name = "usage_active_days_30d", nullable = false)
    private Integer usageActiveDays30d;
}