package org.backend.domain.batch.service;

import lombok.extern.slf4j.Slf4j;
import org.backend.domain.batch.client.PythonInferenceResponse;
import org.backend.domain.batch.pipeline.AnalyticsRunContext;
import org.backend.domain.batch.pipeline.AnalyticsRunStatus;
import org.backend.domain.batch.pipeline.AnalyticsRunType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class SnapshotPublishService {

    private static final String[] STAGING_TABLES = {
            "feature_consultation_staging",
            "feature_lifecycle_staging",
            "feature_monetary_staging",
            "feature_usage_staging"
    };

    private static final String[] VERSIONED_ANALYSIS_TABLES = {
            "ltv_snapshot",
            "cohort_snapshot",
            "conversion_snapshot",
            "churn_snapshot",
            "reason_snapshot",
            "region_snapshot",
            "churn_prediction_snapshot",
            "churn_prediction_summary_snapshot"
    };

    private final JdbcTemplate jdbcTemplate;
    private final String analysisSchema;

    public SnapshotPublishService(
            JdbcTemplate jdbcTemplate,
            @Value("${analytics.analysis-schema:ojo_analysis}") String analysisSchema) {
        if (!analysisSchema.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid analytics analysis schema name");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.analysisSchema = analysisSchema;
    }

    public int validateFeatureStaging(String batchId, int expectedTargetCount) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String table : STAGING_TABLES) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + table + " WHERE batch_id = ?",
                    Integer.class,
                    batchId
            );
            counts.put(table, count == null ? 0 : count);
        }

        boolean complete = counts.values().stream().allMatch(count -> count == expectedTargetCount);
        if (!complete) {
            throw new IllegalStateException(
                    "Feature staging validation failed: expected=%d, actual=%s"
                            .formatted(expectedTargetCount, counts)
            );
        }
        return expectedTargetCount;
    }

    @Transactional
    public void prepareRfmStaging(String batchId) {
        jdbcTemplate.update("""
                INSERT INTO rfm_staging (batch_id, member_id, recency, frequency, monetary)
                SELECT batch_id, member_id,
                       TIMESTAMP(last_payment_date), payment_count_6m, total_revenue
                FROM feature_monetary_staging
                WHERE batch_id = ? AND last_payment_date IS NOT NULL
                ON DUPLICATE KEY UPDATE
                    recency = VALUES(recency), frequency = VALUES(frequency), monetary = VALUES(monetary)
                """, batchId);
    }

    @Transactional
    public void publish(
            AnalyticsRunContext context,
            PythonInferenceResponse inference,
            boolean advanceWatermark) {
        int targetCount = validateFeatureStaging(context.batchId(), targetCount(context.batchId()));

        publishConsultation(context.batchId());
        publishLifecycle(context.batchId());
        publishMonetary(context.batchId());
        publishUsage(context.batchId());
        publishRfmCurrent(context.batchId(), context.runType() == AnalyticsRunType.FULL);
        if (context.runType() == AnalyticsRunType.FULL) {
            publishAnalysis(context.batchId());
        }

        int inferenceSuccess = 0;
        int inferenceFailure = 0;
        String modelVersion = null;
        String snapshotVersion = context.batchId();

        if (inference != null) {
            inferenceSuccess = inference.successCount();
            inferenceFailure = inference.failureCount();
            modelVersion = inference.modelVersion();
            snapshotVersion = inference.snapshotVersion();
            publishChurn(context.batchId(), context.runType() == AnalyticsRunType.FULL);
        }

        if (context.runType() == AnalyticsRunType.FULL) {
            if (inferenceFailure > 0 || inferenceSuccess != targetCount) {
                throw new IllegalStateException("A full run cannot publish incomplete inference results");
            }
            jdbcTemplate.update("UPDATE analytics_snapshot_manifest SET active = FALSE WHERE active = TRUE");
            jdbcTemplate.update("""
                    INSERT INTO analytics_snapshot_manifest (
                        snapshot_version, batch_id, status, data_as_of,
                        model_version, active, activated_at
                    ) VALUES (?, ?, 'READY', ?, ?, TRUE, CURRENT_TIMESTAMP(6))
                    ON DUPLICATE KEY UPDATE
                        status = 'READY', data_as_of = VALUES(data_as_of),
                        model_version = VALUES(model_version), active = TRUE,
                        activated_at = CURRENT_TIMESTAMP(6)
                    """, snapshotVersion, context.batchId(), context.featureBaseAt(), modelVersion);
        }

        if (advanceWatermark) {
            jdbcTemplate.update("""
                    INSERT INTO analytics_watermark (
                        pipeline_name, last_successful_end_at, batch_id
                    ) VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        last_successful_end_at = VALUES(last_successful_end_at),
                        batch_id = VALUES(batch_id)
                    """,
                    "analytics-feature-incremental",
                    context.windowEndAt(),
                    context.batchId()
            );
        }

        AnalyticsRunStatus finalStatus = inferenceFailure == 0
                ? AnalyticsRunStatus.READY
                : AnalyticsRunStatus.READY_WITH_ERRORS;
        jdbcTemplate.update("""
                UPDATE analytics_batch_run
                SET status = ?, feature_success_count = ?,
                    inference_success_count = ?, failure_count = ?,
                    model_version = ?, snapshot_version = ?,
                    completed_at = CURRENT_TIMESTAMP(6)
                WHERE batch_id = ?
                """,
                finalStatus.name(), targetCount,
                inferenceSuccess, inferenceFailure,
                modelVersion, snapshotVersion,
                context.batchId()
        );

        updateTargetStatuses(context.batchId(), inference != null);
        cleanupRetention(context.batchId());

        log.info("Analytics snapshot published - batchId={}, runType={}, targets={}, failures={}",
                context.batchId(), context.runType(), targetCount, inferenceFailure);
    }

    private int targetCount(String batchId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT target_count FROM analytics_batch_run WHERE batch_id = ?",
                Integer.class,
                batchId
        );
        return count == null ? 0 : count;
    }

    private void publishConsultation(String batchId) {
        jdbcTemplate.update("""
                INSERT INTO feature_consultation (
                    member_id, feature_base_date, feature_base_at, batch_id,
                    total_consult_count, last_7d_consult_count, last_30d_consult_count,
                    avg_monthly_consult_count, last_consult_date, top_consult_category,
                    total_complaint_count, last_consult_days_ago,
                    night_consult_count, weekend_consult_count, updated_at
                )
                SELECT member_id, feature_base_date, feature_base_at, batch_id,
                       total_consult_count, last_7d_consult_count, last_30d_consult_count,
                       avg_monthly_consult_count, last_consult_date, top_consult_category,
                       total_complaint_count, last_consult_days_ago,
                       night_consult_count, weekend_consult_count, CURRENT_TIMESTAMP(6)
                FROM feature_consultation_staging WHERE batch_id = ?
                ON DUPLICATE KEY UPDATE
                    feature_base_at = VALUES(feature_base_at), batch_id = VALUES(batch_id),
                    total_consult_count = VALUES(total_consult_count),
                    last_7d_consult_count = VALUES(last_7d_consult_count),
                    last_30d_consult_count = VALUES(last_30d_consult_count),
                    avg_monthly_consult_count = VALUES(avg_monthly_consult_count),
                    last_consult_date = VALUES(last_consult_date),
                    top_consult_category = VALUES(top_consult_category),
                    total_complaint_count = VALUES(total_complaint_count),
                    last_consult_days_ago = VALUES(last_consult_days_ago),
                    night_consult_count = VALUES(night_consult_count),
                    weekend_consult_count = VALUES(weekend_consult_count),
                    updated_at = CURRENT_TIMESTAMP(6)
                """, batchId);
    }

    private void publishLifecycle(String batchId) {
        jdbcTemplate.update("""
                INSERT INTO feature_lifecycle (
                    member_id, feature_base_date, feature_base_at, batch_id, signup_date,
                    member_lifetime_days, is_new_customer_flag, is_dormant_flag,
                    is_terminated_flag, days_since_last_activity,
                    contract_end_days_left, updated_at
                )
                SELECT member_id, feature_base_date, feature_base_at, batch_id, signup_date,
                       member_lifetime_days, is_new_customer_flag, is_dormant_flag,
                       is_terminated_flag, days_since_last_activity,
                       contract_end_days_left, CURRENT_TIMESTAMP(6)
                FROM feature_lifecycle_staging WHERE batch_id = ?
                ON DUPLICATE KEY UPDATE
                    feature_base_at = VALUES(feature_base_at), batch_id = VALUES(batch_id),
                    signup_date = VALUES(signup_date),
                    member_lifetime_days = VALUES(member_lifetime_days),
                    is_new_customer_flag = VALUES(is_new_customer_flag),
                    is_dormant_flag = VALUES(is_dormant_flag),
                    is_terminated_flag = VALUES(is_terminated_flag),
                    days_since_last_activity = VALUES(days_since_last_activity),
                    contract_end_days_left = VALUES(contract_end_days_left),
                    updated_at = CURRENT_TIMESTAMP(6)
                """, batchId);
    }

    private void publishMonetary(String batchId) {
        jdbcTemplate.update("""
                INSERT INTO feature_monetary (
                    member_id, feature_base_date, feature_base_at, batch_id,
                    total_revenue, last_payment_amount, avg_monthly_bill,
                    last_payment_date, payment_count_6m, monthly_revenue,
                    payment_delay_count, prev_monthly_revenue,
                    purchase_cycle, is_vip_prev_month, avg_order_val, updated_at
                )
                SELECT member_id, feature_base_date, feature_base_at, batch_id,
                       total_revenue, last_payment_amount, avg_monthly_bill,
                       last_payment_date, payment_count_6m, monthly_revenue,
                       payment_delay_count, prev_monthly_revenue,
                       purchase_cycle, is_vip_prev_month, avg_order_val, CURRENT_TIMESTAMP(6)
                FROM feature_monetary_staging WHERE batch_id = ?
                ON DUPLICATE KEY UPDATE
                    feature_base_at = VALUES(feature_base_at), batch_id = VALUES(batch_id),
                    total_revenue = VALUES(total_revenue),
                    last_payment_amount = VALUES(last_payment_amount),
                    avg_monthly_bill = VALUES(avg_monthly_bill),
                    last_payment_date = VALUES(last_payment_date),
                    payment_count_6m = VALUES(payment_count_6m),
                    monthly_revenue = VALUES(monthly_revenue),
                    payment_delay_count = VALUES(payment_delay_count),
                    prev_monthly_revenue = VALUES(prev_monthly_revenue),
                    purchase_cycle = VALUES(purchase_cycle),
                    is_vip_prev_month = VALUES(is_vip_prev_month),
                    avg_order_val = VALUES(avg_order_val),
                    updated_at = CURRENT_TIMESTAMP(6)
                """, batchId);
    }

    private void publishUsage(String batchId) {
        jdbcTemplate.update("""
                INSERT INTO feature_usage (
                    member_id, feature_base_date, feature_base_at, batch_id,
                    total_usage_amount, avg_daily_usage, max_usage_amount,
                    usage_peak_hour, premium_service_count,
                    last_activity_date, usage_active_days_30d, updated_at
                )
                SELECT member_id, feature_base_date, feature_base_at, batch_id,
                       total_usage_amount, avg_daily_usage, max_usage_amount,
                       usage_peak_hour, premium_service_count,
                       last_activity_date, usage_active_days_30d, CURRENT_TIMESTAMP(6)
                FROM feature_usage_staging WHERE batch_id = ?
                ON DUPLICATE KEY UPDATE
                    feature_base_at = VALUES(feature_base_at), batch_id = VALUES(batch_id),
                    total_usage_amount = VALUES(total_usage_amount),
                    avg_daily_usage = VALUES(avg_daily_usage),
                    max_usage_amount = VALUES(max_usage_amount),
                    usage_peak_hour = VALUES(usage_peak_hour),
                    premium_service_count = VALUES(premium_service_count),
                    last_activity_date = VALUES(last_activity_date),
                    usage_active_days_30d = VALUES(usage_active_days_30d),
                    updated_at = CURRENT_TIMESTAMP(6)
                """, batchId);
    }

    private void publishRfmCurrent(String batchId, boolean fullRun) {
        if (fullRun) {
            jdbcTemplate.update("DELETE FROM rfm");
        }
        jdbcTemplate.update("""
                INSERT INTO rfm (member_id, recency, frequency, monetary, updated_at)
                SELECT member_id, recency, frequency, monetary, CURRENT_TIMESTAMP(6)
                FROM rfm_staging WHERE batch_id = ?
                ON DUPLICATE KEY UPDATE
                    recency = VALUES(recency), frequency = VALUES(frequency),
                    monetary = VALUES(monetary), updated_at = CURRENT_TIMESTAMP(6)
                """, batchId);
    }

    private void publishChurn(String batchId, boolean fullRun) {
        String staging = analysisSchema + ".churn_prediction_staging";
        String current = analysisSchema + ".churn_prediction_current";
        String history = analysisSchema + ".churn_prediction_history";
        String summary = analysisSchema + ".churn_prediction_summary_current";

        if (fullRun) {
            jdbcTemplate.update("DELETE FROM " + current);
        }

        jdbcTemplate.update("""
                INSERT IGNORE INTO %s (
                    snapshot_version, member_id, churn_score, risk_grade,
                    model_version, data_as_of, created_at
                )
                SELECT snapshot_version, member_id, churn_score, risk_grade,
                       model_version, data_as_of, CURRENT_TIMESTAMP(6)
                FROM %s WHERE batch_id = ? AND status = 'SUCCESS'
                """.formatted(history, staging), batchId);

        jdbcTemplate.update("""
                INSERT INTO %s (
                    member_id, churn_score, risk_grade, model_version,
                    snapshot_version, data_as_of, updated_at
                )
                SELECT member_id, churn_score, risk_grade, model_version,
                       snapshot_version, data_as_of, CURRENT_TIMESTAMP(6)
                FROM %s WHERE batch_id = ? AND status = 'SUCCESS'
                ON DUPLICATE KEY UPDATE
                    churn_score = VALUES(churn_score), risk_grade = VALUES(risk_grade),
                    model_version = VALUES(model_version),
                    snapshot_version = VALUES(snapshot_version),
                    data_as_of = VALUES(data_as_of), updated_at = CURRENT_TIMESTAMP(6)
                """.formatted(current, staging), batchId);

        jdbcTemplate.update("DELETE FROM " + summary);
        jdbcTemplate.update("""
                INSERT INTO %s (risk_grade, customer_count, ratio, data_as_of)
                SELECT risk_grade, COUNT(*),
                       ROUND(COUNT(*) * 100.0 / totals.total_count, 2),
                       MAX(data_as_of)
                FROM %s
                CROSS JOIN (SELECT COUNT(*) AS total_count FROM %s) totals
                GROUP BY risk_grade, totals.total_count
                """.formatted(summary, current, current));
    }

    private void publishAnalysis(String batchId) {
        jdbcTemplate.update("DELETE FROM analysis_current");
        jdbcTemplate.update("""
                INSERT IGNORE INTO analysis_history (
                    snapshot_version, member_id, rfm_score, type, ltv,
                    lifecycle_stage, r_score, f_score, m_score, data_as_of
                )
                SELECT snapshot_version, member_id, rfm_score, type, ltv,
                       lifecycle_stage, r_score, f_score, m_score, data_as_of
                FROM analysis_staging WHERE batch_id = ?
                """, batchId);

        jdbcTemplate.update("""
                INSERT INTO analysis_current (
                    member_id, snapshot_version, rfm_score, type, ltv,
                    lifecycle_stage, r_score, f_score, m_score, data_as_of
                )
                SELECT member_id, snapshot_version, rfm_score, type, ltv,
                       lifecycle_stage, r_score, f_score, m_score, data_as_of
                FROM analysis_staging WHERE batch_id = ?
                ON DUPLICATE KEY UPDATE
                    snapshot_version = VALUES(snapshot_version),
                    rfm_score = VALUES(rfm_score), type = VALUES(type), ltv = VALUES(ltv),
                    lifecycle_stage = VALUES(lifecycle_stage),
                    r_score = VALUES(r_score), f_score = VALUES(f_score), m_score = VALUES(m_score),
                    data_as_of = VALUES(data_as_of), updated_at = CURRENT_TIMESTAMP(6)
                """, batchId);

        jdbcTemplate.update("""
                INSERT INTO analysis (
                    member_id, rfm_score, type, ltv, lifecycle_stage,
                    created_at, r_score, f_score, m_score
                )
                SELECT member_id, rfm_score, type, ltv, lifecycle_stage,
                       data_as_of, r_score, f_score, m_score
                FROM analysis_staging WHERE batch_id = ?
                """, batchId);
    }

    private void updateTargetStatuses(String batchId, boolean inferenceRan) {
        if (!inferenceRan) {
            jdbcTemplate.update("""
                    UPDATE analytics_batch_target
                    SET status = 'FEATURE_READY'
                    WHERE batch_id = ?
                    """, batchId);
            return;
        }

        String staging = analysisSchema + ".churn_prediction_staging";
        jdbcTemplate.update("""
                UPDATE analytics_batch_target target
                LEFT JOIN %s inference
                  ON inference.batch_id = target.batch_id
                 AND inference.member_id = target.member_id
                SET target.status = CASE
                        WHEN target.inference_required = FALSE THEN 'FEATURE_READY'
                        WHEN inference.status = 'SUCCESS' THEN 'INFERENCE_READY'
                        ELSE 'FAILED'
                    END,
                    target.retry_count = CASE
                        WHEN inference.status = 'FAILED'
                         AND FIND_IN_SET('RETRY', REPLACE(target.reason, ', ', ',')) > 0
                        THEN target.retry_count + 1
                        ELSE target.retry_count
                    END,
                    target.error_message = inference.error_message
                WHERE target.batch_id = ?
                """.formatted(staging), batchId);
    }

    private void cleanupRetention(String currentBatchId) {
        for (String table : STAGING_TABLES) {
            jdbcTemplate.update(
                    "DELETE FROM " + table + " WHERE created_at < UTC_TIMESTAMP(6) - INTERVAL 7 DAY AND batch_id <> ?",
                    currentBatchId
            );
        }
        jdbcTemplate.update("""
                DELETE FROM rfm_staging
                WHERE created_at < UTC_TIMESTAMP(6) - INTERVAL 7 DAY AND batch_id <> ?
                """, currentBatchId);
        jdbcTemplate.update("""
                DELETE FROM analysis_staging
                WHERE created_at < UTC_TIMESTAMP(6) - INTERVAL 7 DAY AND batch_id <> ?
                """, currentBatchId);
        jdbcTemplate.update(
                "DELETE FROM " + analysisSchema + ".churn_prediction_staging " +
                        "WHERE created_at < UTC_TIMESTAMP(6) - INTERVAL 7 DAY AND batch_id <> ?",
                currentBatchId
        );

        jdbcTemplate.update("""
                DELETE history
                FROM analysis_history history
                LEFT JOIN (
                    SELECT snapshot_version
                    FROM analytics_snapshot_manifest
                    WHERE status = 'READY'
                    ORDER BY activated_at DESC
                    LIMIT 2
                ) retained ON retained.snapshot_version = history.snapshot_version
                WHERE retained.snapshot_version IS NULL
                """);

        jdbcTemplate.update("""
                DELETE history
                FROM %s history
                LEFT JOIN (
                    SELECT snapshot_version
                    FROM analytics_snapshot_manifest
                    WHERE status = 'READY'
                    ORDER BY activated_at DESC
                    LIMIT 2
                ) retained ON retained.snapshot_version = history.snapshot_version
                WHERE history.created_at < UTC_TIMESTAMP(6) - INTERVAL 7 DAY
                  AND retained.snapshot_version IS NULL
                """.formatted(analysisSchema + ".churn_prediction_history"));

        jdbcTemplate.update("""
                DELETE manifest
                FROM analytics_snapshot_manifest manifest
                LEFT JOIN (
                    SELECT snapshot_version FROM (
                        SELECT snapshot_version
                        FROM analytics_snapshot_manifest
                        WHERE status = 'READY'
                        ORDER BY activated_at DESC
                        LIMIT 2
                    ) latest
                ) retained ON retained.snapshot_version = manifest.snapshot_version
                WHERE manifest.active = FALSE AND retained.snapshot_version IS NULL
                """);

        for (String table : VERSIONED_ANALYSIS_TABLES) {
            if (!analysisTableExists(table)) {
                continue;
            }
            jdbcTemplate.update("""
                    DELETE FROM %s.%s
                    WHERE snapshot_version IS NOT NULL
                      AND snapshot_version NOT IN (
                          SELECT snapshot_version FROM (
                              SELECT snapshot_version
                              FROM analytics_snapshot_manifest
                              WHERE status = 'READY'
                              ORDER BY activated_at DESC
                              LIMIT 2
                          ) retained
                      )
                    """.formatted(analysisSchema, table));
        }
    }

    private boolean analysisTableExists(String table) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = ? AND table_name = ?
                        """,
                Integer.class,
                analysisSchema,
                table
        );
        return count != null && count > 0;
    }
}
