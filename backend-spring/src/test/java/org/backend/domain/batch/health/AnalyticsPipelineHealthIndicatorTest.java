package org.backend.domain.batch.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsPipelineHealthIndicatorTest {

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-07-18T12:00:00Z"), ZoneOffset.UTC
    );

    @Test
    void reportsUpWhenLatestFullSnapshotIsFresh() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForMap(anyString())).thenReturn(Map.of(
                "last_ready_at", Timestamp.valueOf("2026-07-18 11:00:00")
        ));

        var indicator = new AnalyticsPipelineHealthIndicator(
                jdbcTemplate, clock, Duration.ofHours(30)
        );

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void reportsDownWhenFailureOccurredAfterLatestReadySnapshot() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForMap(anyString())).thenReturn(Map.of(
                "last_ready_at", Timestamp.valueOf("2026-07-18 10:00:00"),
                "last_failed_at", Timestamp.valueOf("2026-07-18 11:00:00")
        ));

        var indicator = new AnalyticsPipelineHealthIndicator(
                jdbcTemplate, clock, Duration.ofHours(30)
        );

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
    }
}
