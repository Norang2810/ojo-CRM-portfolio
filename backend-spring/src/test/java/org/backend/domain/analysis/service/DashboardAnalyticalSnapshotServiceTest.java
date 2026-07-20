package org.backend.domain.analysis.service;

import org.backend.domain.analysis.entity.DashboardSummaryStats;
import org.backend.domain.analysis.repository.AnalyticsSnapshotManifestRepository;
import org.backend.domain.analysis.repository.DashboardDailyStatsRepository;
import org.backend.domain.analysis.repository.DashboardSummaryStatsRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardAnalyticalSnapshotServiceTest {

    @Test
    void refusesToCacheOldStatsUnderNewActiveVersion() {
        DashboardSummaryStatsRepository summaryRepository = mock(DashboardSummaryStatsRepository.class);
        DashboardDailyStatsRepository dailyRepository = mock(DashboardDailyStatsRepository.class);
        AnalyticsSnapshotManifestRepository manifestRepository = mock(AnalyticsSnapshotManifestRepository.class);
        DashboardSummaryStats oldSummary = mock(DashboardSummaryStats.class);
        when(oldSummary.getSnapshotVersion()).thenReturn("snapshot-old");
        when(summaryRepository.findTopByOrderByStatDateDesc()).thenReturn(Optional.of(oldSummary));

        DashboardAnalyticalSnapshotService service = new DashboardAnalyticalSnapshotService(
                summaryRepository,
                dailyRepository,
                manifestRepository
        );

        assertThatThrownBy(() -> service.getSnapshot("snapshot-new"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not ready");
    }
}
