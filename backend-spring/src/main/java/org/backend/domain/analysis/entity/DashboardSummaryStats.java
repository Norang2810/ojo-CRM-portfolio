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
@Table(name = "dashboard_summary_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DashboardSummaryStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stat_date", nullable = false, unique = true)
    private LocalDate statDate;

    @Column(name = "current_customers", nullable = false)
    private Long currentCustomers;

    @Column(name = "current_customers_change_rate", nullable = false)
    private Double currentCustomersChangeRate;

    @Column(name = "new_active_customers", nullable = false)
    private Long newActiveCustomers;

    @Column(name = "new_active_customers_change_rate", nullable = false)
    private Double newActiveCustomersChangeRate;

    @Column(name = "new_customers", nullable = false)
    private Long newCustomers;

    @Column(name = "new_customers_change_rate", nullable = false)
    private Double newCustomersChangeRate;

    @Column(name = "at_risk_customers", nullable = false)
    private Long atRiskCustomers;

    @Column(name = "at_risk_customers_change_rate", nullable = false)
    private Double atRiskCustomersChangeRate;

    @Column(name = "vip_count", nullable = false)
    private Long vipCount;

    @Column(name = "potential_vip_count", nullable = false)
    private Long potentialVipCount;

    @Column(name = "general_count", nullable = false)
    private Long generalCount;

    @Column(name = "at_risk_count", nullable = false)
    private Long atRiskCount;

    @Column(name = "churned_count", nullable = false)
    private Long churnedCount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    private DashboardSummaryStats(
            LocalDate statDate,
            Long currentCustomers,
            Double currentCustomersChangeRate,
            Long newActiveCustomers,
            Double newActiveCustomersChangeRate,
            Long newCustomers,
            Double newCustomersChangeRate,
            Long atRiskCustomers,
            Double atRiskCustomersChangeRate,
            Long vipCount,
            Long potentialVipCount,
            Long generalCount,
            Long atRiskCount,
            Long churnedCount
    ) {
        this.statDate = statDate;
        this.currentCustomers = currentCustomers;
        this.currentCustomersChangeRate = currentCustomersChangeRate;
        this.newActiveCustomers = newActiveCustomers;
        this.newActiveCustomersChangeRate = newActiveCustomersChangeRate;
        this.newCustomers = newCustomers;
        this.newCustomersChangeRate = newCustomersChangeRate;
        this.atRiskCustomers = atRiskCustomers;
        this.atRiskCustomersChangeRate = atRiskCustomersChangeRate;
        this.vipCount = vipCount;
        this.potentialVipCount = potentialVipCount;
        this.generalCount = generalCount;
        this.atRiskCount = atRiskCount;
        this.churnedCount = churnedCount;
    }

    public void update(
            Long currentCustomers,
            Double currentCustomersChangeRate,
            Long newActiveCustomers,
            Double newActiveCustomersChangeRate,
            Long newCustomers,
            Double newCustomersChangeRate,
            Long atRiskCustomers,
            Double atRiskCustomersChangeRate,
            Long vipCount,
            Long potentialVipCount,
            Long generalCount,
            Long atRiskCount,
            Long churnedCount
    ) {
        this.currentCustomers = currentCustomers;
        this.currentCustomersChangeRate = currentCustomersChangeRate;
        this.newActiveCustomers = newActiveCustomers;
        this.newActiveCustomersChangeRate = newActiveCustomersChangeRate;
        this.newCustomers = newCustomers;
        this.newCustomersChangeRate = newCustomersChangeRate;
        this.atRiskCustomers = atRiskCustomers;
        this.atRiskCustomersChangeRate = atRiskCustomersChangeRate;
        this.vipCount = vipCount;
        this.potentialVipCount = potentialVipCount;
        this.generalCount = generalCount;
        this.atRiskCount = atRiskCount;
        this.churnedCount = churnedCount;
    }
}