package org.backend.domain.batch.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Component("analyticsPipeline")
public class AnalyticsPipelineHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    private final Duration maxFullAge;

    public AnalyticsPipelineHealthIndicator(
            JdbcTemplate jdbcTemplate,
            Clock clock,
            @Value("${analytics.health.max-full-age:30h}") Duration maxFullAge) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.maxFullAge = maxFullAge;
    }

    @Override
    public Health health() {
        Map<String, Object> state = jdbcTemplate.queryForMap("""
                SELECT
                    MAX(CASE
                        WHEN run_type = 'FULL' AND status IN ('READY', 'READY_WITH_ERRORS')
                        THEN completed_at
                    END) AS last_ready_at,
                    MAX(CASE WHEN status = 'FAILED' THEN completed_at END) AS last_failed_at
                FROM analytics_batch_run
                """);

        LocalDateTime lastReadyAt = toDateTime(state.get("last_ready_at"));
        LocalDateTime lastFailedAt = toDateTime(state.get("last_failed_at"));

        if (lastReadyAt == null) {
            Health.Builder builder = lastFailedAt == null ? Health.unknown() : Health.down();
            builder.withDetail("reason", "No READY full analytics snapshot exists");
            if (lastFailedAt != null) {
                builder.withDetail("lastFailedAt", lastFailedAt);
            }
            return builder.build();
        }

        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        Duration age = Duration.between(lastReadyAt, now);
        boolean failedAfterReady = lastFailedAt != null && lastFailedAt.isAfter(lastReadyAt);
        boolean stale = age.compareTo(maxFullAge) > 0;

        Health.Builder builder = failedAfterReady || stale ? Health.down() : Health.up();
        builder.withDetail("lastReadyAt", lastReadyAt)
                .withDetail("snapshotAgeSeconds", Math.max(0, age.toSeconds()))
                .withDetail("maxSnapshotAgeSeconds", maxFullAge.toSeconds())
                .withDetail("failedAfterReady", failedAfterReady);
        if (lastFailedAt != null) {
            builder.withDetail("lastFailedAt", lastFailedAt);
        }
        return builder.build();
    }

    private LocalDateTime toDateTime(Object value) {
        return value instanceof Timestamp timestamp ? timestamp.toLocalDateTime() : null;
    }
}
