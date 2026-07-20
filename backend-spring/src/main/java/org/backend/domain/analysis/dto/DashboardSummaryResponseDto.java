package org.backend.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.time.OffsetDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponseDto {
    private DashboardCardsDto cards;
    private List<DashboardDailyStatDto> dailyStats;
    private DashboardSegmentStatDto segments;
    private String snapshotVersion;
    private OffsetDateTime analyticalDataAsOf;
    private OffsetDateTime operationalDataAsOf;
    private boolean stale;
}
