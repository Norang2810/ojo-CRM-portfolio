package org.backend.domain.analysis.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.backend.domain.analysis.dto.DashboardCardsDto;
import org.backend.domain.analysis.dto.DashboardSegmentStatDto;
import org.backend.domain.analysis.dto.DashboardSummaryResponseDto;
import org.backend.domain.analysis.repository.AnalyticsSnapshotManifestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardAnalyticalSnapshotService analyticalSnapshotService;
    @Mock
    private DashboardOperationalService operationalService;
    @Mock
    private AnalyticsSnapshotManifestRepository manifestRepository;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache staleCache;

    @Test
    void returnsMarkedStaleSnapshotWhenFreshDatabaseReadFails() {
        DashboardService service = new DashboardService(
                analyticalSnapshotService,
                operationalService,
                manifestRepository,
                cacheManager,
                new SimpleMeterRegistry()
        );
        DashboardSummaryResponseDto cached = DashboardSummaryResponseDto.builder()
                .cards(DashboardCardsDto.builder().build())
                .segments(DashboardSegmentStatDto.builder().build())
                .snapshotVersion("snapshot-previous")
                .analyticalDataAsOf(LocalDateTime.of(2026, 7, 17, 15, 0).atOffset(ZoneOffset.UTC))
                .operationalDataAsOf(LocalDateTime.of(2026, 7, 18, 3, 0).atOffset(ZoneOffset.UTC))
                .stale(false)
                .build();

        when(manifestRepository.findActive()).thenThrow(new IllegalStateException("database unavailable"));
        when(cacheManager.getCache("dashboardStaleCache")).thenReturn(staleCache);
        when(staleCache.get("latest", DashboardSummaryResponseDto.class)).thenReturn(cached);

        DashboardSummaryResponseDto response = service.getDashboardSummary();

        assertThat(response.isStale()).isTrue();
        assertThat(response.getSnapshotVersion()).isEqualTo("snapshot-previous");
        assertThat(response.getAnalyticalDataAsOf()).isEqualTo(cached.getAnalyticalDataAsOf());
    }
}
