package org.backend.domain.analysis.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardAnalyticalSnapshot(
        DashboardCardsDto cards,
        List<DashboardDailyStatDto> dailyStats,
        DashboardSegmentStatDto segments,
        String snapshotVersion,
        LocalDateTime dataAsOf
) {
}
