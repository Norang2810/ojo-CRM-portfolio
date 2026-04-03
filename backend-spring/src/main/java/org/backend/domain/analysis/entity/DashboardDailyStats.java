package org.backend.domain.analysis.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "dashboard_daily_stats",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_dashboard_daily_stats_stat_date", columnNames = "stat_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DashboardDailyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "new_customers", nullable = false)
    private Long newCustomers;

    @Column(name = "churned_customers", nullable = false)
    private Long churnedCustomers;

    @Column(name = "active_customers", nullable = false)
    private Long activeCustomers;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    private DashboardDailyStats(
            LocalDate statDate,
            Long newCustomers,
            Long churnedCustomers,
            Long activeCustomers
    ) {
        this.statDate = statDate;
        this.newCustomers = newCustomers;
        this.churnedCustomers = churnedCustomers;
        this.activeCustomers = activeCustomers;
    }

    public void update(Long newCustomers, Long churnedCustomers, Long activeCustomers) {
        this.newCustomers = newCustomers;
        this.churnedCustomers = churnedCustomers;
        this.activeCustomers = activeCustomers;
    }
}