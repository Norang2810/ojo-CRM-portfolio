package org.backend.domain.analysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.domain.analysis.dto.DashboardAnalyticalSnapshot;
import org.backend.domain.analysis.dto.DashboardCardsDto;
import org.backend.domain.analysis.dto.DashboardDailyStatDto;
import org.backend.domain.analysis.dto.DashboardSegmentStatDto;
import org.backend.domain.analysis.entity.DashboardSummaryStats;
import org.backend.domain.analysis.repository.AnalyticsSnapshotManifestRepository;
import org.backend.domain.analysis.repository.DashboardDailyStatsRepository;
import org.backend.domain.analysis.repository.DashboardSummaryStatsRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardAnalyticalSnapshotService {

    private final DashboardSummaryStatsRepository dashboardSummaryStatsRepository;
    private final DashboardDailyStatsRepository dashboardDailyStatsRepository;
    private final AnalyticsSnapshotManifestRepository manifestRepository;

    @Cacheable(value = "dashboardAnalyticCache", key = "#snapshotVersion", sync = true)
    public DashboardAnalyticalSnapshot getSnapshot(String snapshotVersion) {
        DashboardSummaryStats summary = dashboardSummaryStatsRepository.findTopByOrderByStatDateDesc()
                .orElseThrow(() -> new IllegalStateException("Dashboard analytical snapshot does not exist"));
        requireVersion(snapshotVersion, summary.getSnapshotVersion());

        LocalDate endDate = summary.getStatDate();
        var dailyRows = dashboardDailyStatsRepository
                .findByStatDateBetweenOrderByStatDateAsc(endDate.minusDays(6), endDate);
        if (dailyRows.stream().anyMatch(row -> !matchesVersion(snapshotVersion, row.getSnapshotVersion()))) {
            throw new IllegalStateException(
                    "Dashboard daily stats do not match active snapshot: " + snapshotVersion
            );
        }
        var dailyStats = dailyRows.stream()
                .map(daily -> new DashboardDailyStatDto(
                        daily.getStatDate().toString(),
                        daily.getNewCustomers(),
                        daily.getChurnedCustomers(),
                        daily.getActiveCustomers()
                ))
                .toList();

        DashboardCardsDto cards = DashboardCardsDto.builder()
                .currentCustomers(new DashboardCardsDto.CardStat(
                        summary.getCurrentCustomers(), summary.getCurrentCustomersChangeRate()))
                .newActiveCustomers(new DashboardCardsDto.CardStat(
                        summary.getNewActiveCustomers(), summary.getNewActiveCustomersChangeRate()))
                .newCustomers(new DashboardCardsDto.CardStat(
                        summary.getNewCustomers(), summary.getNewCustomersChangeRate()))
                .atRiskCustomers(new DashboardCardsDto.CardStat(
                        summary.getAtRiskCustomers(), summary.getAtRiskCustomersChangeRate()))
                .build();

        DashboardSegmentStatDto segments = DashboardSegmentStatDto.builder()
                .vip(summary.getVipCount())
                .potentialVip(summary.getPotentialVipCount())
                .general(summary.getGeneralCount())
                .atRisk(summary.getAtRiskCount())
                .churned(summary.getChurnedCount())
                .build();

        var manifest = manifestRepository.findReadyVersion(snapshotVersion);
        var dataAsOf = manifest.map(AnalyticsSnapshotManifestRepository.ActiveSnapshot::dataAsOf)
                .orElse(summary.getComputedAt() != null ? summary.getComputedAt() : summary.getUpdatedAt());

        log.info("Dashboard analytical snapshot loaded - snapshotVersion={}", snapshotVersion);
        return new DashboardAnalyticalSnapshot(cards, dailyStats, segments, snapshotVersion, dataAsOf);
    }

    private void requireVersion(String requestedVersion, String storedVersion) {
        if (!matchesVersion(requestedVersion, storedVersion)) {
            throw new IllegalStateException(
                    "Dashboard stats are not ready for active snapshot: " + requestedVersion
            );
        }
    }

    private boolean matchesVersion(String requestedVersion, String storedVersion) {
        return requestedVersion.equals(storedVersion)
                || ("legacy".equals(requestedVersion)
                && (storedVersion == null || "legacy".equals(storedVersion)));
    }
}
