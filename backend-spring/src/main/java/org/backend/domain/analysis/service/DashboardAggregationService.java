package org.backend.domain.analysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.backend.domain.analysis.dto.projection.DashboardDailyCountProjection;
import org.backend.domain.analysis.dto.projection.DashboardSegmentCountProjection;
import org.backend.domain.analysis.entity.DashboardDailyStats;
import org.backend.domain.analysis.entity.DashboardSummaryStats;
import org.backend.domain.analysis.repository.DashboardDailyStatsRepository;
import org.backend.domain.analysis.repository.DashboardRepository;
import org.backend.domain.analysis.repository.DashboardSummaryStatsRepository;
import org.backend.domain.analysis.repository.AnalyticsSnapshotManifestRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardAggregationService {

    private final DashboardRepository dashboardRepository;
    private final DashboardSummaryStatsRepository dashboardSummaryStatsRepository;
    private final DashboardDailyStatsRepository dashboardDailyStatsRepository;
    private final DashboardService dashboardService;
    private final CacheManager cacheManager;
    private final AnalyticsSnapshotManifestRepository manifestRepository;
    private final Clock clock;

    @Value("${analytics.time-zone:Asia/Seoul}")
    private String businessTimeZone;

    @Transactional
    @SchedulerLock(
            name = "dashboard-refresh",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT30S"
    )
    public void refreshDashboardStats() {
        ZoneId zone = ZoneId.of(businessTimeZone);
        LocalDate today = LocalDate.now(clock.withZone(zone));
        var activeSnapshot = manifestRepository.findActive();
        String snapshotVersion = activeSnapshot
                .map(AnalyticsSnapshotManifestRepository.ActiveSnapshot::version)
                .orElse("legacy");
        LocalDateTime computedAt = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        LocalDate startOfThisMonth = today.withDayOfMonth(1);

        LocalDate startOfThisWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate startOfLastWeek = startOfThisWeek.minusWeeks(1);
        LocalDate endOfLastWeek = startOfThisWeek.minusDays(1);

        String todayExclusive = toUtc(today.plusDays(1), zone);
        String monthStartStr = toUtc(startOfThisMonth, zone);
        String thisWeekStartStr = toUtc(startOfThisWeek, zone);
        String lastWeekStartStr = toUtc(startOfLastWeek, zone);
        String lastWeekEndStr = toUtc(endOfLastWeek.plusDays(1), zone);

        long currentTotal = dashboardRepository.countCurrentCustomers(todayExclusive);
        long lastWeekTotal = dashboardRepository.countCurrentCustomers(lastWeekEndStr);

        long newActiveThisMonth = dashboardRepository.countNewActiveCustomers(
                startOfThisMonth.toString(), today.plusDays(1).toString());
        long newActiveThisWeek = dashboardRepository.countNewActiveCustomers(
                startOfThisWeek.toString(), today.plusDays(1).toString());
        long newActiveLastWeek = dashboardRepository.countNewActiveCustomers(
                startOfLastWeek.toString(), endOfLastWeek.plusDays(1).toString());

        long newThisMonth = dashboardRepository.countNewCustomers(monthStartStr, todayExclusive);
        long newThisWeek = dashboardRepository.countNewCustomers(thisWeekStartStr, todayExclusive);
        long newLastWeek = dashboardRepository.countNewCustomers(lastWeekStartStr, lastWeekEndStr);

        long riskThisMonth = dashboardRepository.countAtRiskCustomers(monthStartStr, todayExclusive);
        long riskThisWeek = dashboardRepository.countAtRiskCustomers(thisWeekStartStr, todayExclusive);
        long riskLastWeek = dashboardRepository.countAtRiskCustomers(lastWeekStartStr, lastWeekEndStr);

        List<DashboardSegmentCountProjection> segmentsDb = dashboardRepository.getSegmentCounts();
        long vip = 0L;
        long potentialVip = 0L;
        long general = 0L;
        long atRisk = 0L;
        long churned = 0L;

        for (DashboardSegmentCountProjection row : segmentsDb) {
            String type = row.getType() == null ? "COMMON" : row.getType();
            long count = row.getCountValue();

            switch (type) {
                case "VIP" -> vip += count;
                case "LOYAL" -> potentialVip += count;
                case "COMMON" -> general += count;
                case "RISK", "SLEEP" -> atRisk += count;
                case "LOST" -> churned += count;
                default -> {
                }
            }
        }

        DashboardSummaryStats summary = dashboardSummaryStatsRepository.findByStatDate(today)
                .orElseGet(() -> DashboardSummaryStats.builder()
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
                        .build());

        summary.update(
                currentTotal,
                calculateChange(currentTotal, lastWeekTotal),
                newActiveThisMonth,
                calculateChange(newActiveThisWeek, newActiveLastWeek),
                newThisMonth,
                calculateChange(newThisWeek, newLastWeek),
                riskThisMonth,
                calculateChange(riskThisWeek, riskLastWeek),
                vip,
                potentialVip,
                general,
                atRisk,
                churned
        );
        summary.markComputed(snapshotVersion, computedAt);

        dashboardSummaryStatsRepository.save(summary);

        LocalDate sevenDaysAgo = today.minusDays(6);
        String sevenDaysAgoStr = toUtc(sevenDaysAgo, zone);

        Map<String, Long> mappedNew = parseDaily(dashboardRepository.getDailyNewCustomers(sevenDaysAgoStr, todayExclusive));
        Map<String, Long> mappedChurn = parseDaily(dashboardRepository.getDailyChurnedCustomers(sevenDaysAgoStr, todayExclusive));
        Map<String, Long> mappedActive = parseDaily(dashboardRepository.getDailyActiveCustomers(
                sevenDaysAgo.toString(), today.plusDays(1).toString()));

        for (int i = 0; i <= 6; i++) {
            LocalDate statDate = sevenDaysAgo.plusDays(i);
            String key = statDate.toString();

            DashboardDailyStats dailyStats = dashboardDailyStatsRepository.findByStatDate(statDate)
                    .orElseGet(() -> DashboardDailyStats.builder()
                            .statDate(statDate)
                            .newCustomers(0L)
                            .churnedCustomers(0L)
                            .activeCustomers(0L)
                            .build());

            dailyStats.update(
                    mappedNew.getOrDefault(key, 0L),
                    mappedChurn.getOrDefault(key, 0L),
                    mappedActive.getOrDefault(key, 0L)
            );
            dailyStats.markComputed(snapshotVersion, computedAt);

            dashboardDailyStatsRepository.save(dailyStats);
        }

        scheduleDashboardCacheRefreshAfterCommit();
        log.info("Dashboard precomputed stats refresh completed - statDate={}", today);
    }

    private double calculateChange(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return Math.round((((double) current - previous) / previous) * 1000.0) / 10.0;
    }

    private Map<String, Long> parseDaily(List<DashboardDailyCountProjection> rows) {
        Map<String, Long> map = new HashMap<>();
        for (DashboardDailyCountProjection row : rows) {
            map.put(row.getStatDate().toString(), row.getCountValue());
        }
        return map;
    }

    private String toUtc(LocalDate businessDate, ZoneId zone) {
        LocalDateTime utc = LocalDateTime.ofInstant(
                businessDate.atStartOfDay(zone).toInstant(),
                ZoneOffset.UTC
        );
        return utc.toString();
    }

    private void scheduleDashboardCacheRefreshAfterCommit() {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                String version = manifestRepository.findActive()
                        .map(AnalyticsSnapshotManifestRepository.ActiveSnapshot::version)
                        .orElse("legacy");
                Cache cache = cacheManager.getCache("dashboardAnalyticCache");
                if (cache != null) {
                    cache.evict(version);
                }
                try {
                    dashboardService.getDashboardSummary();
                    log.info("Dashboard version cache warmed after aggregation commit - snapshotVersion={}", version);
                } catch (RuntimeException warmFailure) {
                    log.error("Dashboard cache warm failed; DB/stale fallback remains available - snapshotVersion={}",
                            version, warmFailure);
                }
            }
        });
    }
}
