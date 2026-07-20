package org.backend.domain.batch.repository;

import lombok.RequiredArgsConstructor;
import org.backend.domain.batch.pipeline.AnalyticsRunContext;
import org.backend.domain.batch.pipeline.AnalyticsRunStatus;
import org.backend.domain.batch.pipeline.AnalyticsRunType;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AnalyticsBatchRunRepository {

    public static final String INCREMENTAL_WATERMARK = "analytics-feature-incremental";

    private final JdbcTemplate jdbcTemplate;

    public boolean create(AnalyticsRunContext context) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO analytics_batch_run (
                        batch_id, run_type, feature_base_date, feature_base_at,
                        window_start_at, window_end_at, status
                    ) VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    context.batchId(),
                    context.runType().name(),
                    context.featureBaseDate(),
                    Timestamp.valueOf(context.featureBaseAt()),
                    Timestamp.valueOf(context.windowStartAt()),
                    Timestamp.valueOf(context.windowEndAt()),
                    AnalyticsRunStatus.BUILDING.name()
            );
            return true;
        } catch (DuplicateKeyException duplicateWindow) {
            return false;
        }
    }

    public int addAllMembers(String batchId) {
        jdbcTemplate.update("""
                INSERT IGNORE INTO analytics_batch_target (
                    batch_id, member_id, inference_required, reason, status
                )
                SELECT ?, member_id, TRUE, 'FULL', 'PENDING'
                FROM member
                """, batchId);
        return refreshTargetCount(batchId);
    }

    public int refreshTargetCount(String batchId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM analytics_batch_target WHERE batch_id = ?",
                Integer.class,
                batchId
        );
        int targetCount = count == null ? 0 : count;
        jdbcTemplate.update(
                "UPDATE analytics_batch_run SET target_count = ? WHERE batch_id = ?",
                targetCount,
                batchId
        );
        return targetCount;
    }

    public int targetCount(String batchId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT target_count FROM analytics_batch_run WHERE batch_id = ?",
                Integer.class,
                batchId
        );
        return count == null ? 0 : count;
    }

    public int inferenceTargetCount(String batchId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM analytics_batch_target
                        WHERE batch_id = ? AND inference_required = TRUE
                        """,
                Integer.class,
                batchId
        );
        return count == null ? 0 : count;
    }

    public void disableInference(String batchId) {
        jdbcTemplate.update("""
                UPDATE analytics_batch_target
                SET inference_required = FALSE
                WHERE batch_id = ?
                """, batchId);
    }

    public void updateStatus(String batchId, AnalyticsRunStatus status) {
        jdbcTemplate.update(
                "UPDATE analytics_batch_run SET status = ? WHERE batch_id = ?",
                status.name(),
                batchId
        );
    }

    public void markFeatureReady(String batchId, int featureCount) {
        jdbcTemplate.update("""
                UPDATE analytics_batch_run
                SET status = ?, feature_success_count = ?
                WHERE batch_id = ?
                """, AnalyticsRunStatus.FEATURE_READY.name(), featureCount, batchId);
    }

    public void markFailed(String batchId, Throwable failure) {
        String message = failure == null ? "Unknown analytics pipeline failure" : failure.getMessage();
        if (message != null && message.length() > 4000) {
            message = message.substring(0, 4000);
        }
        jdbcTemplate.update("""
                UPDATE analytics_batch_run
                SET status = ?, error_message = ?, completed_at = CURRENT_TIMESTAMP(6)
                WHERE batch_id = ?
                """, AnalyticsRunStatus.FAILED.name(), message, batchId);
    }

    public void markReadyWithWarning(String batchId, Throwable warning) {
        String message = warning == null ? "Dashboard refresh failed after snapshot publish" : warning.getMessage();
        if (message != null && message.length() > 4000) {
            message = message.substring(0, 4000);
        }
        jdbcTemplate.update("""
                UPDATE analytics_batch_run
                SET status = ?, error_message = ?
                WHERE batch_id = ? AND status = ?
                """,
                AnalyticsRunStatus.READY_WITH_ERRORS.name(),
                message,
                batchId,
                AnalyticsRunStatus.READY.name()
        );
    }

    public Optional<LocalDateTime> lastSuccessfulEndAt() {
        return jdbcTemplate.query("""
                        SELECT last_successful_end_at
                        FROM analytics_watermark
                        WHERE pipeline_name = ?
                        """,
                resultSet -> resultSet.next()
                        ? Optional.of(resultSet.getTimestamp(1).toLocalDateTime())
                        : Optional.empty(),
                INCREMENTAL_WATERMARK
        );
    }

    public Optional<AnalyticsRunContext> findContext(String batchId) {
        return jdbcTemplate.query("""
                        SELECT batch_id, run_type, feature_base_date, feature_base_at,
                               window_start_at, window_end_at
                        FROM analytics_batch_run WHERE batch_id = ?
                        """,
                resultSet -> resultSet.next()
                        ? Optional.of(new AnalyticsRunContext(
                                resultSet.getString("batch_id"),
                                AnalyticsRunType.valueOf(resultSet.getString("run_type")),
                                resultSet.getDate("feature_base_date").toLocalDate(),
                                resultSet.getTimestamp("feature_base_at").toLocalDateTime(),
                                resultSet.getTimestamp("window_start_at").toLocalDateTime(),
                                resultSet.getTimestamp("window_end_at").toLocalDateTime()
                        ))
                        : Optional.empty(),
                batchId
        );
    }

    public boolean resetFailedForRetry(String batchId) {
        return jdbcTemplate.update("""
                UPDATE analytics_batch_run
                SET status = ?, error_message = NULL, completed_at = NULL
                WHERE batch_id = ? AND status = ?
                """,
                AnalyticsRunStatus.BUILDING.name(),
                batchId,
                AnalyticsRunStatus.FAILED.name()
        ) == 1;
    }
}
