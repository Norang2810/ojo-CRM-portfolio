package org.backend.domain.batch.config.writer;

import org.backend.domain.batch.entity.ConsultationBasics;
import org.backend.domain.batch.entity.FeatureUsage;
import org.backend.domain.batch.entity.Lifecycle;
import org.backend.domain.batch.entity.Monetary;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class MemberFeatureWriterConfig {

    @Bean
    @StepScope
    public JdbcBatchItemWriter<ConsultationBasics> consultationWriter(
            DataSource dataSource,
            @Value("#{jobParameters['batchId']}") String batchId,
            @Value("#{jobParameters['staged']}") String staged) {
        boolean staging = Boolean.parseBoolean(staged) && hasBatchId(batchId);
        String table = staging ? "feature_consultation_staging" : "feature_consultation";
        String keyColumns = staging ? "batch_id, " : "";
        String keyValues = staging ? ":batchId, " : "";

        return new JdbcBatchItemWriterBuilder<ConsultationBasics>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO %s (
                            %smember_id, feature_base_date, feature_base_at,
                            total_consult_count, last_7d_consult_count, last_30d_consult_count,
                            avg_monthly_consult_count, last_consult_date, top_consult_category,
                            total_complaint_count, last_consult_days_ago,
                            night_consult_count, weekend_consult_count%s
                        ) VALUES (
                            %s:memberId, :featureBaseDate, :featureBaseAt,
                            :totalConsultCount, :last7dConsultCount, :last30dConsultCount,
                            :avgMonthlyConsultCount, :lastConsultDate, :topConsultCategory,
                            :totalComplaintCount, :lastConsultDaysAgo,
                            :nightConsultCount, :weekendConsultCount%s
                        )
                        ON DUPLICATE KEY UPDATE
                            feature_base_at          = VALUES(feature_base_at),
                            total_consult_count      = VALUES(total_consult_count),
                            last_7d_consult_count    = VALUES(last_7d_consult_count),
                            last_30d_consult_count   = VALUES(last_30d_consult_count),
                            avg_monthly_consult_count= VALUES(avg_monthly_consult_count),
                            last_consult_date        = VALUES(last_consult_date),
                            top_consult_category     = VALUES(top_consult_category),
                            total_complaint_count    = VALUES(total_complaint_count),
                            last_consult_days_ago    = VALUES(last_consult_days_ago),
                            night_consult_count      = VALUES(night_consult_count),
                            weekend_consult_count    = VALUES(weekend_consult_count)
                        """.formatted(
                        table,
                        keyColumns,
                        staging ? "" : ", batch_id, updated_at",
                        keyValues,
                        staging ? "" : ", :batchId, CURRENT_TIMESTAMP(6)"
                ))
                .beanMapped()
                .build();
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<Lifecycle> lifecycleWriter(
            DataSource dataSource,
            @Value("#{jobParameters['batchId']}") String batchId,
            @Value("#{jobParameters['staged']}") String staged) {
        boolean staging = Boolean.parseBoolean(staged) && hasBatchId(batchId);
        String table = staging ? "feature_lifecycle_staging" : "feature_lifecycle";

        return new JdbcBatchItemWriterBuilder<Lifecycle>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO %s (
                            %smember_id, feature_base_date, feature_base_at, signup_date,
                            member_lifetime_days, is_new_customer_flag,
                            is_dormant_flag, is_terminated_flag,
                            days_since_last_activity, contract_end_days_left%s
                        ) VALUES (%s?, ?, ?, ?, ?, ?, ?, ?, ?, ?%s)
                        ON DUPLICATE KEY UPDATE
                            feature_base_at          = VALUES(feature_base_at),
                            signup_date              = VALUES(signup_date),
                            member_lifetime_days     = VALUES(member_lifetime_days),
                            is_new_customer_flag     = VALUES(is_new_customer_flag),
                            is_dormant_flag          = VALUES(is_dormant_flag),
                            is_terminated_flag       = VALUES(is_terminated_flag),
                            days_since_last_activity = VALUES(days_since_last_activity),
                            contract_end_days_left   = VALUES(contract_end_days_left)
                        """.formatted(
                        table,
                        staging ? "batch_id, " : "",
                        staging ? "" : ", batch_id, updated_at",
                        staging ? "?, " : "",
                        staging ? "" : ", ?, CURRENT_TIMESTAMP(6)"
                ))
                .itemPreparedStatementSetter((item, ps) -> {
                    int index = 1;
                    if (staging) ps.setString(index++, item.getBatchId());
                    ps.setLong(index++, item.getMemberId());
                    ps.setObject(index++, item.getFeatureBaseDate());
                    ps.setObject(index++, item.getFeatureBaseAt());
                    ps.setObject(index++, item.getSignupDate());
                    ps.setInt(index++, item.getMemberLifetimeDays());
                    ps.setString(index++, yesNo(item.getIsNewCustomerFlag()));
                    ps.setString(index++, yesNo(item.getIsDormantFlag()));
                    ps.setString(index++, yesNo(item.getIsTerminatedFlag()));
                    ps.setObject(index++, item.getDaysSinceLastActivity());
                    ps.setObject(index++, item.getContractEndDaysLeft());
                    if (!staging) ps.setString(index, item.getBatchId());
                })
                .build();
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<Monetary> monetaryWriter(
            DataSource dataSource,
            @Value("#{jobParameters['batchId']}") String batchId,
            @Value("#{jobParameters['staged']}") String staged) {
        boolean staging = Boolean.parseBoolean(staged) && hasBatchId(batchId);
        String table = staging ? "feature_monetary_staging" : "feature_monetary";

        return new JdbcBatchItemWriterBuilder<Monetary>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO %s (
                            %smember_id, feature_base_date, feature_base_at,
                            total_revenue, last_payment_amount, avg_monthly_bill,
                            last_payment_date, payment_count_6m, monthly_revenue,
                            payment_delay_count, prev_monthly_revenue,
                            purchase_cycle, is_vip_prev_month, avg_order_val%s
                        ) VALUES (%s?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?%s)
                        ON DUPLICATE KEY UPDATE
                            feature_base_at          = VALUES(feature_base_at),
                            total_revenue            = VALUES(total_revenue),
                            last_payment_amount      = VALUES(last_payment_amount),
                            avg_monthly_bill         = VALUES(avg_monthly_bill),
                            last_payment_date        = VALUES(last_payment_date),
                            payment_count_6m         = VALUES(payment_count_6m),
                            monthly_revenue          = VALUES(monthly_revenue),
                            payment_delay_count      = VALUES(payment_delay_count),
                            prev_monthly_revenue     = VALUES(prev_monthly_revenue),
                            purchase_cycle           = VALUES(purchase_cycle),
                            is_vip_prev_month        = VALUES(is_vip_prev_month),
                            avg_order_val            = VALUES(avg_order_val)
                        """.formatted(
                        table,
                        staging ? "batch_id, " : "",
                        staging ? "" : ", batch_id, updated_at",
                        staging ? "?, " : "",
                        staging ? "" : ", ?, CURRENT_TIMESTAMP(6)"
                ))
                .itemPreparedStatementSetter((item, ps) -> {
                    int index = 1;
                    if (staging) ps.setString(index++, item.getBatchId());
                    ps.setLong(index++, item.getMemberId());
                    ps.setObject(index++, item.getFeatureBaseDate());
                    ps.setObject(index++, item.getFeatureBaseAt());
                    ps.setLong(index++, item.getTotalRevenue());
                    ps.setLong(index++, item.getLastPaymentAmount());
                    ps.setFloat(index++, item.getAvgMonthlyBill());
                    ps.setObject(index++, item.getLastPaymentDate());
                    ps.setInt(index++, item.getPaymentCount6m());
                    ps.setLong(index++, item.getMonthlyRevenue());
                    ps.setInt(index++, item.getPaymentDelayCount());
                    ps.setLong(index++, item.getPrevMonthlyRevenue());
                    ps.setInt(index++, item.getPurchaseCycle());
                    ps.setString(index++, yesNo(item.getVipPrevMonth()));
                    ps.setFloat(index++, item.getAvgOrderVal());
                    if (!staging) ps.setString(index, item.getBatchId());
                })
                .build();
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<FeatureUsage> usageWriter(
            DataSource dataSource,
            @Value("#{jobParameters['batchId']}") String batchId,
            @Value("#{jobParameters['staged']}") String staged) {
        boolean staging = Boolean.parseBoolean(staged) && hasBatchId(batchId);
        String table = staging ? "feature_usage_staging" : "feature_usage";
        String keyColumns = staging ? "batch_id, " : "";
        String keyValues = staging ? ":batchId, " : "";

        return new JdbcBatchItemWriterBuilder<FeatureUsage>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO %s (
                            %smember_id, feature_base_date, feature_base_at,
                            total_usage_amount, avg_daily_usage, max_usage_amount,
                            usage_peak_hour, premium_service_count,
                            last_activity_date, usage_active_days_30d%s
                        ) VALUES (
                            %s:memberId, :featureBaseDate, :featureBaseAt,
                            :totalUsageAmount, :avgDailyUsage, :maxUsageAmount,
                            :usagePeakHour, :premiumServiceCount,
                            :lastActivityDate, :usageActiveDays30d%s
                        )
                        ON DUPLICATE KEY UPDATE
                            feature_base_at          = VALUES(feature_base_at),
                            total_usage_amount       = VALUES(total_usage_amount),
                            avg_daily_usage          = VALUES(avg_daily_usage),
                            max_usage_amount         = VALUES(max_usage_amount),
                            usage_peak_hour          = VALUES(usage_peak_hour),
                            premium_service_count    = VALUES(premium_service_count),
                            last_activity_date       = VALUES(last_activity_date),
                            usage_active_days_30d    = VALUES(usage_active_days_30d)
                        """.formatted(
                        table,
                        keyColumns,
                        staging ? "" : ", batch_id, updated_at",
                        keyValues,
                        staging ? "" : ", :batchId, CURRENT_TIMESTAMP(6)"
                ))
                .beanMapped()
                .build();
    }

    private boolean hasBatchId(String batchId) {
        return batchId != null && !batchId.isBlank();
    }

    private String yesNo(Boolean value) {
        return Boolean.TRUE.equals(value) ? "Y" : "N";
    }
}
