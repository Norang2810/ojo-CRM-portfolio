package org.backend.domain.analysis.scheduler;

import lombok.RequiredArgsConstructor;
import org.backend.domain.analysis.service.DashboardAggregationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "dashboard.refresh.enabled", havingValue = "true")
public class DashboardRefreshScheduler {

    private final DashboardAggregationService dashboardAggregationService;

    @Scheduled(cron = "${dashboard.refresh.cron}", zone = "${analytics.time-zone:Asia/Seoul}")
    public void refresh() {
        dashboardAggregationService.refreshDashboardStats();
    }
}
