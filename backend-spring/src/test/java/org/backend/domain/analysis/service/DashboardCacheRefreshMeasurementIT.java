package org.backend.domain.analysis.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.TimeUnit;

@SpringBootTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=update")
class DashboardCacheRefreshMeasurementIT {

    @Autowired
    private DashboardAggregationService dashboardAggregationService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void measureCacheStateAroundDashboardRefresh() {
        Cache cache = cacheManager.getCache("dashboardCache");
        if (cache == null) {
            throw new IllegalStateException("dashboardCache is not configured");
        }

        cache.clear();

        dashboardAggregationService.refreshDashboardStats();
        dashboardService.getDashboardSummary();

        boolean keyExistsBeforeSecondRefresh = cache.get("summary") != null;

        dashboardAggregationService.refreshDashboardStats();
        boolean keyExistsImmediatelyAfterRefresh = cache.get("summary") != null;

        long startedAt = System.nanoTime();
        dashboardService.getDashboardSummary();
        long firstRequestMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        boolean keyExistsAfterFirstRequest = cache.get("summary") != null;

        System.out.printf(
                "MEASUREMENT keyBeforeSecondRefresh=%s keyImmediatelyAfterRefresh=%s firstRequestMillis=%d keyAfterFirstRequest=%s%n",
                keyExistsBeforeSecondRefresh,
                keyExistsImmediatelyAfterRefresh,
                firstRequestMillis,
                keyExistsAfterFirstRequest
        );
    }
}
