package org.backend.domain.batch.job.rfm.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class KpiTasklet implements Tasklet {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception{
        log.info("kpi 집계 시작");

        String baseMonth = (String)chunkContext.getStepContext().getJobParameters().get("baseMonth");
        String baseMonthClean = baseMonth.replace("-", "");

        // 중복 실행 방지 - skip vs over write
        jdbcTemplate.update("DELETE FROM rfm_kpi WHERE base_month = ?", baseMonthClean);

        // baseMonth 기준 analysis가 존재해야 함
        // crr = analysis.type vip인 수/feature_monetary.is_vip_prev_month true인 수
        // churn rate = feature_lifecycle.is_terminated_flag true / 전체 고객 수
        // nrr = monthly_revenue/prev_monthly_revenue (당월 매출/지난달 매출)
        String sql = "Insert INTO rfm_kpi(crr, churn_rate, nrr, base_month)" +
                    "Select " +
                    // CRR
                    "CASE " +
                        "WHEN COUNT(CASE WHEN fm.is_vip_prev_month = 'Y' THEN 1 END) = 0 THEN 0.0 "+
                        "ELSE CAST( COUNT("+
                            "CASE WHEN a.type = 'VIP' AND fm.is_vip_prev_month = 'Y' THEN 1 END"+
                            ") AS Decimal(10,4) )"+
                            "/ COUNT(CASE WHEN fm.is_vip_prev_month = 'Y' THEN 1 END) "+
                    "END AS crr, " +
                    // Churn Rate
                    "COALESCE("+
                        "CAST( COUNT("+
                            "CASE WHEN fl.is_terminated_flag = 'Y' THEN 1 END" +
                        ") AS Decimal(10,4) ) "+
                        "/ NULLIF(COUNT(*), 0) "+
                    ", 0.0) AS churn_rate, "+
                    // NRR
                    "CASE "+
                        "WHEN SUM(fm.prev_monthly_revenue) = 0 THEN 0.0 "+
                        "ELSE SUM(fm.monthly_revenue) / SUM(fm.prev_monthly_revenue) "  +
                    "END AS nrr, "+
                    "? "+
                    "From feature_monetary fm " +
                    "JOIN analysis a ON fm.member_id = a.member_id " +
                    "AND DATE_FORMAT(a.created_at, '%Y%m') = ? " +
                    "JOIN feature_lifecycle fl ON fm.member_id = fl.member_id";

        jdbcTemplate.update(sql, baseMonthClean, baseMonthClean);

        log.info("kpi 집계 완료: {}", baseMonthClean);
        return RepeatStatus.FINISHED;
    }
}
