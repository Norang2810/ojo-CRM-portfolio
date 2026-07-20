package org.backend.domain.batch.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class ChangedMemberRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public int findAndStoreChangedMembers(String batchId, LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("batchId", batchId)
                .addValue("from", fromInclusive)
                .addValue("to", toExclusive);

        jdbcTemplate.update("""
                INSERT IGNORE INTO analytics_batch_target (
                    batch_id, member_id, inference_required, reason, status
                )
                SELECT
                    :batchId,
                    changed.member_id,
                    MAX(changed.inference_required),
                    LEFT(GROUP_CONCAT(DISTINCT changed.reason ORDER BY changed.reason), 255),
                    'PENDING'
                FROM (
                    SELECT member_id,
                           IF(status IN ('DORMANT', 'TERMINATED'), 1, 0) AS inference_required,
                           'MEMBER_STATUS' AS reason
                    FROM member
                    WHERE COALESCE(updated_at, created_at) >= :from
                      AND COALESCE(updated_at, created_at) < :to

                    UNION ALL

                    SELECT a.member_id,
                           IF(LOWER(CONCAT_WS(' ', c.category_name, a.advice_content))
                                  REGEXP 'cancel|termination|overdue|refund|churn|해지|미납|연체|환불', 1, 0),
                           'ADVICE' AS reason
                    FROM advice a
                    JOIN categories c ON c.category_id = a.category_id
                    WHERE COALESCE(a.updated_at, a.created_at) >= :from
                      AND COALESCE(a.updated_at, a.created_at) < :to

                    UNION ALL

                    SELECT member_id, 1, 'INVOICE'
                    FROM invoice
                    WHERE COALESCE(updated_at, created_at) >= :from
                      AND COALESCE(updated_at, created_at) < :to

                    UNION ALL

                    SELECT i.member_id, 1, 'PAYMENT'
                    FROM payment p
                    JOIN invoice i ON i.invoice_id = p.invoice_id
                    WHERE COALESCE(p.updated_at, p.created_at, p.paid_at) >= :from
                      AND COALESCE(p.updated_at, p.created_at, p.paid_at) < :to

                    UNION ALL

                    SELECT member_id, 0, 'USAGE'
                    FROM data_usage
                    WHERE COALESCE(updated_at, created_at) >= :from
                      AND COALESCE(updated_at, created_at) < :to

                    UNION ALL

                    SELECT member_id,
                           IF(status IN ('CANCELED', 'CANCELLED', 'TERMINATED'), 1, 0),
                           'SUBSCRIPTION'
                    FROM subscription_period
                    WHERE updated_at >= :from
                      AND updated_at < :to

                    UNION ALL

                    SELECT failed.member_id, 1, 'RETRY'
                    FROM analytics_batch_target failed
                    JOIN analytics_batch_run failed_run
                      ON failed_run.batch_id = failed.batch_id
                    WHERE failed.status = 'FAILED'
                      AND failed.retry_count < 3
                      AND NOT EXISTS (
                          SELECT 1
                          FROM analytics_batch_target newer
                          JOIN analytics_batch_run newer_run
                            ON newer_run.batch_id = newer.batch_id
                          WHERE newer.member_id = failed.member_id
                            AND (
                                newer_run.started_at > failed_run.started_at
                                OR (newer_run.started_at = failed_run.started_at
                                    AND newer.batch_id > failed.batch_id)
                            )
                      )
                ) changed
                GROUP BY changed.member_id
                """, parameters);

        jdbcTemplate.update("""
                UPDATE analytics_batch_target current_target
                JOIN (
                    SELECT member_id, MAX(retry_count) AS previous_retry_count
                    FROM analytics_batch_target
                    WHERE batch_id <> :batchId AND status = 'FAILED'
                    GROUP BY member_id
                ) previous_target
                  ON previous_target.member_id = current_target.member_id
                SET current_target.retry_count = previous_target.previous_retry_count
                WHERE current_target.batch_id = :batchId
                  AND FIND_IN_SET('RETRY', REPLACE(current_target.reason, ', ', ',')) > 0
                """, parameters);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM analytics_batch_target WHERE batch_id = :batchId",
                parameters,
                Integer.class
        );
        return count == null ? 0 : count;
    }
}
