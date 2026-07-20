package org.backend.domain.batch.service;

import org.backend.domain.batch.pipeline.AnalyticsRunContext;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsPipelineServiceTest {

    @Test
    void monthStartUsesPreviousBusinessDateForFeatureAndBaseMonth() {
        Clock clock = Clock.fixed(Instant.parse("2026-02-28T17:00:00Z"), ZoneOffset.UTC);

        AnalyticsRunContext context = AnalyticsPipelineService.buildDailyFullContext(
                clock,
                ZoneId.of("Asia/Seoul")
        );

        assertThat(context.featureBaseDate()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(AnalyticsPipelineService.baseMonthFor(context.featureBaseDate()))
                .isEqualTo("2026-02");
        assertThat(context.windowStartAt())
                .isEqualTo(LocalDateTime.of(2026, 2, 27, 15, 0));
        assertThat(context.windowEndAt())
                .isEqualTo(LocalDateTime.of(2026, 2, 28, 15, 0));
        assertThat(context.featureBaseAt()).isEqualTo(context.windowEndAt());
    }
}
