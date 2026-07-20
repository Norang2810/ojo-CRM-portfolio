package org.backend.domain.analysis.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.domain.analysis.dto.DashboardAnalyticalSnapshot;
import org.backend.domain.analysis.dto.DashboardCardsDto;
import org.backend.domain.analysis.dto.DashboardDailyStatDto;
import org.backend.domain.analysis.dto.DashboardOperationalSnapshot;
import org.backend.domain.analysis.dto.DashboardSummaryResponseDto;
import org.backend.domain.analysis.repository.AnalyticsSnapshotManifestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardAnalyticalSnapshotService analyticalSnapshotService;
    private final DashboardOperationalService operationalService;
    private final AnalyticsSnapshotManifestRepository manifestRepository;
    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;

    @Value("${dashboard.operational-overlay.enabled:true}")
    private boolean operationalOverlayEnabled;

    @Value("${analytics.time-zone:Asia/Seoul}")
    private String businessTimeZone;

    public DashboardSummaryResponseDto getDashboardSummary() {
        try {
            String snapshotVersion = manifestRepository.findActive()
                    .map(AnalyticsSnapshotManifestRepository.ActiveSnapshot::version)
                    .orElse("legacy");
            DashboardAnalyticalSnapshot analytical = analyticalSnapshotService.getSnapshot(snapshotVersion);
            DashboardOperationalSnapshot operational = operationalOverlayEnabled
                    ? operationalService.getCurrent()
                    : null;

            DashboardSummaryResponseDto response = merge(analytical, operational, false);
            putStale(response);
            meterRegistry.counter("cache.gets", "cache", "dashboard", "result", "fresh").increment();
            return response;
        } catch (RuntimeException freshReadFailure) {
            DashboardSummaryResponseDto stale = getStale();
            if (stale != null) {
                meterRegistry.counter("cache.gets", "cache", "dashboard", "result", "stale").increment();
                log.warn("Dashboard fresh read failed; returning stale snapshot", freshReadFailure);
                return DashboardSummaryResponseDto.builder()
                        .cards(stale.getCards())
                        .dailyStats(stale.getDailyStats())
                        .segments(stale.getSegments())
                        .snapshotVersion(stale.getSnapshotVersion())
                        .analyticalDataAsOf(stale.getAnalyticalDataAsOf())
                        .operationalDataAsOf(stale.getOperationalDataAsOf())
                        .stale(true)
                        .build();
            }
            throw freshReadFailure;
        }
    }

    private DashboardSummaryResponseDto merge(
            DashboardAnalyticalSnapshot analytical,
            DashboardOperationalSnapshot operational,
            boolean stale) {
        DashboardCardsDto cards = analytical.cards();
        var dailyStats = new ArrayList<>(analytical.dailyStats());
        var operationalDataAsOf = analytical.dataAsOf();

        if (operational != null) {
            cards = DashboardCardsDto.builder()
                    .currentCustomers(new DashboardCardsDto.CardStat(
                            operational.currentCustomers(),
                            analytical.cards().getCurrentCustomers().getPercentChange()))
                    .newActiveCustomers(analytical.cards().getNewActiveCustomers())
                    .newCustomers(analytical.cards().getNewCustomers())
                    .atRiskCustomers(analytical.cards().getAtRiskCustomers())
                    .build();

            ZoneId zone = ZoneId.of(businessTimeZone);
            String today = operational.dataAsOf()
                    .atOffset(ZoneOffset.UTC)
                    .atZoneSameInstant(zone)
                    .toLocalDate()
                    .toString();
            long activeCustomers = dailyStats.stream()
                    .filter(row -> row.getDate().equals(today))
                    .mapToLong(DashboardDailyStatDto::getActiveCustomers)
                    .findFirst()
                    .orElse(0L);
            dailyStats.removeIf(row -> row.getDate().equals(today));
            dailyStats.add(new DashboardDailyStatDto(
                    today,
                    operational.todayNewCustomers(),
                    operational.todayChurnedCustomers(),
                    activeCustomers
            ));
            dailyStats.sort(Comparator.comparing(DashboardDailyStatDto::getDate));
            while (dailyStats.size() > 7) {
                dailyStats.remove(0);
            }
            operationalDataAsOf = operational.dataAsOf();
        }

        return DashboardSummaryResponseDto.builder()
                .cards(cards)
                .dailyStats(dailyStats)
                .segments(analytical.segments())
                .snapshotVersion(analytical.snapshotVersion())
                .analyticalDataAsOf(toUtcOffset(analytical.dataAsOf()))
                .operationalDataAsOf(toUtcOffset(operationalDataAsOf))
                .stale(stale)
                .build();
    }

    private OffsetDateTime toUtcOffset(LocalDateTime utcDateTime) {
        return utcDateTime == null ? null : utcDateTime.atOffset(ZoneOffset.UTC);
    }

    private void putStale(DashboardSummaryResponseDto response) {
        try {
            Cache cache = cacheManager.getCache("dashboardStaleCache");
            if (cache != null) cache.put("latest", response);
        } catch (RuntimeException cacheFailure) {
            meterRegistry.counter("cache.put.failure", "cache", "dashboardStaleCache").increment();
            log.warn("Could not update dashboard stale cache", cacheFailure);
        }
    }

    private DashboardSummaryResponseDto getStale() {
        try {
            Cache cache = cacheManager.getCache("dashboardStaleCache");
            return cache == null ? null : cache.get("latest", DashboardSummaryResponseDto.class);
        } catch (RuntimeException cacheFailure) {
            meterRegistry.counter("cache.get.failure", "cache", "dashboardStaleCache").increment();
            log.warn("Could not read dashboard stale cache", cacheFailure);
            return null;
        }
    }
}
