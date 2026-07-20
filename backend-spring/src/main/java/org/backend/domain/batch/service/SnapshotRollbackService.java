package org.backend.domain.batch.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SnapshotRollbackService {

    private final JdbcTemplate jdbcTemplate;
    private final String analysisSchema;

    public SnapshotRollbackService(
            JdbcTemplate jdbcTemplate,
            @Value("${analytics.analysis-schema:ojo_analysis}") String analysisSchema) {
        if (!analysisSchema.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid analytics analysis schema name");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.analysisSchema = analysisSchema;
    }

    @Transactional
    public void activate(String snapshotVersion) {
        String batchId = jdbcTemplate.query("""
                        SELECT batch_id FROM analytics_snapshot_manifest
                        WHERE snapshot_version = ? AND status = 'READY'
                        """,
                resultSet -> {
                    if (!resultSet.next()) {
                        throw new IllegalArgumentException("READY snapshot does not exist: " + snapshotVersion);
                    }
                    return resultSet.getString("batch_id");
                },
                snapshotVersion
        );

        jdbcTemplate.update("DELETE FROM analysis_current");
        jdbcTemplate.update("""
                INSERT INTO analysis_current (
                    member_id, snapshot_version, rfm_score, type, ltv,
                    lifecycle_stage, r_score, f_score, m_score, data_as_of
                )
                SELECT member_id, snapshot_version, rfm_score, type, ltv,
                       lifecycle_stage, r_score, f_score, m_score, data_as_of
                FROM analysis_history WHERE snapshot_version = ?
                """, snapshotVersion);
        jdbcTemplate.update("""
                INSERT INTO analysis (
                    member_id, rfm_score, type, ltv, lifecycle_stage,
                    created_at, r_score, f_score, m_score
                )
                SELECT member_id, rfm_score, type, ltv, lifecycle_stage,
                       CURRENT_TIMESTAMP(6), r_score, f_score, m_score
                FROM analysis_history WHERE snapshot_version = ?
                """, snapshotVersion);

        jdbcTemplate.update("DELETE FROM rfm");
        jdbcTemplate.update("""
                INSERT INTO rfm (member_id, recency, frequency, monetary, updated_at)
                SELECT member_id, recency, frequency, monetary, CURRENT_TIMESTAMP(6)
                FROM rfm_staging WHERE batch_id = ?
                """, batchId);

        String current = analysisSchema + ".churn_prediction_current";
        String history = analysisSchema + ".churn_prediction_history";
        String summary = analysisSchema + ".churn_prediction_summary_current";
        jdbcTemplate.update("DELETE FROM " + current);
        jdbcTemplate.update("""
                INSERT INTO %s (
                    member_id, churn_score, risk_grade, model_version,
                    snapshot_version, data_as_of
                )
                SELECT member_id, churn_score, risk_grade, model_version,
                       snapshot_version, data_as_of
                FROM %s WHERE snapshot_version = ?
                """.formatted(current, history), snapshotVersion);
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

        jdbcTemplate.update("UPDATE analytics_snapshot_manifest SET active = FALSE WHERE active = TRUE");
        jdbcTemplate.update("""
                UPDATE analytics_snapshot_manifest
                SET active = TRUE, activated_at = CURRENT_TIMESTAMP(6)
                WHERE snapshot_version = ?
                """, snapshotVersion);
    }
}
