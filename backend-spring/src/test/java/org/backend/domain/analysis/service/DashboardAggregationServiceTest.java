package org.backend.domain.analysis.service;

import org.backend.domain.analysis.dto.projection.DashboardDailyCountProjection;
import org.backend.domain.analysis.dto.projection.DashboardSegmentCountProjection;
import org.backend.domain.analysis.entity.DashboardSummaryStats;
import org.backend.domain.analysis.repository.DashboardDailyStatsRepository;
import org.backend.domain.analysis.repository.DashboardRepository;
import org.backend.domain.analysis.repository.DashboardSummaryStatsRepository;
import org.backend.domain.analysis.repository.AnalyticsSnapshotManifestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardAggregationServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;
    @Mock
    private DashboardSummaryStatsRepository dashboardSummaryStatsRepository;
    @Mock
    private DashboardDailyStatsRepository dashboardDailyStatsRepository;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;

    private DashboardAggregationService dashboardAggregationService;
    @Mock
    private AnalyticsSnapshotManifestRepository manifestRepository;

    @BeforeEach
    void setUp() {
        dashboardAggregationService = new DashboardAggregationService(
                dashboardRepository,
                dashboardSummaryStatsRepository,
                dashboardDailyStatsRepository,
                dashboardService,
                cacheManager,
                manifestRepository,
                Clock.fixed(Instant.parse("2026-07-18T03:00:00Z"), ZoneOffset.UTC)
        );
        ReflectionTestUtils.setField(dashboardAggregationService, "businessTimeZone", "Asia/Seoul");
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void refreshesDashboardCacheOnlyAfterSuccessfulCommit() {
        LocalDate today = LocalDate.of(2026, 7, 18);
        DashboardSummaryStats existingSummary = DashboardSummaryStats.builder()
                .statDate(today)
                .currentCustomers(0L)
                .currentCustomersChangeRate(0.0)
                .newActiveCustomers(0L)
                .newActiveCustomersChangeRate(0.0)
                .newCustomers(0L)
                .newCustomersChangeRate(0.0)
                .atRiskCustomers(0L)
                .atRiskCustomersChangeRate(0.0)
                .vipCount(0L)
                .potentialVipCount(0L)
                .generalCount(0L)
                .atRiskCount(0L)
                .churnedCount(0L)
                .build();

        when(dashboardRepository.countCurrentCustomers(any())).thenReturn(10L);
        when(dashboardRepository.countNewActiveCustomers(any(), any())).thenReturn(2L);
        when(dashboardRepository.countNewCustomers(any(), any())).thenReturn(3L);
        when(dashboardRepository.countAtRiskCustomers(any(), any())).thenReturn(1L);
        when(dashboardRepository.getSegmentCounts()).thenReturn(List.of(segment("VIP", 2L)));
        when(dashboardRepository.getDailyNewCustomers(any(), any())).thenReturn(List.of(daily(today, 3L)));
        when(dashboardRepository.getDailyChurnedCustomers(any(), any())).thenReturn(List.of());
        when(dashboardRepository.getDailyActiveCustomers(any(), any())).thenReturn(List.of(daily(today, 8L)));
        when(dashboardSummaryStatsRepository.findByStatDate(today)).thenReturn(Optional.of(existingSummary));
        when(dashboardDailyStatsRepository.findByStatDate(any())).thenReturn(Optional.empty());
        when(manifestRepository.findActive()).thenReturn(Optional.empty());
        when(cacheManager.getCache("dashboardAnalyticCache")).thenReturn(cache);

        TransactionSynchronizationManager.initSynchronization();

        dashboardAggregationService.refreshDashboardStats();

        verify(cache, never()).evict(any());
        verify(dashboardService, never()).getDashboardSummary();

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCommit());

        verify(cache).evict("legacy");
        verify(dashboardService).getDashboardSummary();
    }

    private DashboardSegmentCountProjection segment(String type, Long count) {
        return new DashboardSegmentCountProjection() {
            @Override
            public String getType() {
                return type;
            }

            @Override
            public Long getCountValue() {
                return count;
            }
        };
    }

    private DashboardDailyCountProjection daily(LocalDate date, Long count) {
        return new DashboardDailyCountProjection() {
            @Override
            public LocalDate getStatDate() {
                return date;
            }

            @Override
            public Long getCountValue() {
                return count;
            }
        };
    }
}
