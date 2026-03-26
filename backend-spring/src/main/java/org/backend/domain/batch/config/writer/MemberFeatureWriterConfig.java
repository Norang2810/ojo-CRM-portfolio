package org.backend.domain.batch.config.writer;

import org.backend.domain.batch.entity.ConsultationBasics;
import org.backend.domain.batch.entity.FeatureUsage;
import org.backend.domain.batch.entity.Lifecycle;
import org.backend.domain.batch.entity.Monetary;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class MemberFeatureWriterConfig {

    @Bean
    public JdbcBatchItemWriter<ConsultationBasics> consultationWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<ConsultationBasics>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO feature_consultation (
                            member_id, feature_base_date,
                            total_consult_count, last_7d_consult_count, last_30d_consult_count,
                            avg_monthly_consult_count, last_consult_date, top_consult_category,
                            total_complaint_count, last_consult_days_ago,
                            night_consult_count, weekend_consult_count
                        ) VALUES (
                            :memberId, :featureBaseDate,
                            :totalConsultCount, :last7dConsultCount, :last30dConsultCount,
                            :avgMonthlyConsultCount, :lastConsultDate, :topConsultCategory,
                            :totalComplaintCount, :lastConsultDaysAgo,
                            :nightConsultCount, :weekendConsultCount
                        )
                        ON DUPLICATE KEY UPDATE
                            total_consult_count       = VALUES(total_consult_count),
                            last_7d_consult_count     = VALUES(last_7d_consult_count),
                            last_30d_consult_count    = VALUES(last_30d_consult_count),
                            avg_monthly_consult_count = VALUES(avg_monthly_consult_count),
                            last_consult_date         = VALUES(last_consult_date),
                            top_consult_category      = VALUES(top_consult_category),
                            total_complaint_count     = VALUES(total_complaint_count),
                            last_consult_days_ago     = VALUES(last_consult_days_ago),
                            night_consult_count       = VALUES(night_consult_count),
                            weekend_consult_count     = VALUES(weekend_consult_count)
                        """)
                .beanMapped()
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Lifecycle> lifecycleWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Lifecycle>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO feature_lifecycle (
                            member_id, feature_base_date, signup_date,
                            member_lifetime_days, is_new_customer_flag,
                            is_dormant_flag, is_terminated_flag,
                            days_since_last_activity, contract_end_days_left
                        ) VALUES (
                            ?, ?, ?, ?, ?, ?, ?, ?, ?
                        )
                        ON DUPLICATE KEY UPDATE
                            signup_date              = VALUES(signup_date),
                            member_lifetime_days     = VALUES(member_lifetime_days),
                            is_new_customer_flag     = VALUES(is_new_customer_flag),
                            is_dormant_flag          = VALUES(is_dormant_flag),
                            is_terminated_flag       = VALUES(is_terminated_flag),
                            days_since_last_activity = VALUES(days_since_last_activity),
                            contract_end_days_left   = VALUES(contract_end_days_left)
                        """)
                // ✅ YesNoConverter(Y/N) Boolean 필드는 beanMapped() 처리 불가 → 직접 매핑
                .itemPreparedStatementSetter((item, ps) -> {
                    ps.setLong(1, item.getMemberId());
                    ps.setObject(2, item.getFeatureBaseDate());
                    ps.setObject(3, item.getSignupDate());
                    ps.setInt(4, item.getMemberLifetimeDays());
                    ps.setString(5, Boolean.TRUE.equals(item.getIsNewCustomerFlag()) ? "Y" : "N");
                    ps.setString(6, Boolean.TRUE.equals(item.getIsDormantFlag()) ? "Y" : "N");
                    ps.setString(7, Boolean.TRUE.equals(item.getIsTerminatedFlag()) ? "Y" : "N");
                    ps.setObject(8, item.getDaysSinceLastActivity());
                    ps.setObject(9, item.getContractEndDaysLeft());
                })
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Monetary> monetaryWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Monetary>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO feature_monetary (
                            member_id, feature_base_date,
                            total_revenue, last_payment_amount, avg_monthly_bill,
                            last_payment_date, payment_count_6m, monthly_revenue,
                            payment_delay_count, prev_monthly_revenue,
                            purchase_cycle, is_vip_prev_month, avg_order_val
                        ) VALUES (
                            ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                        )
                        ON DUPLICATE KEY UPDATE
                            total_revenue        = VALUES(total_revenue),
                            last_payment_amount  = VALUES(last_payment_amount),
                            avg_monthly_bill     = VALUES(avg_monthly_bill),
                            last_payment_date    = VALUES(last_payment_date),
                            payment_count_6m     = VALUES(payment_count_6m),
                            monthly_revenue      = VALUES(monthly_revenue),
                            payment_delay_count  = VALUES(payment_delay_count),
                            prev_monthly_revenue = VALUES(prev_monthly_revenue),
                            purchase_cycle       = VALUES(purchase_cycle),
                            is_vip_prev_month    = VALUES(is_vip_prev_month),
                            avg_order_val        = VALUES(avg_order_val)
                        """)
                // ✅ is_vip_prev_month YesNoConverter Boolean → 직접 매핑
                .itemPreparedStatementSetter((item, ps) -> {
                    ps.setLong(1, item.getMemberId());
                    ps.setObject(2, item.getFeatureBaseDate());
                    ps.setLong(3, item.getTotalRevenue());
                    ps.setLong(4, item.getLastPaymentAmount());
                    ps.setFloat(5, item.getAvgMonthlyBill());
                    ps.setObject(6, item.getLastPaymentDate());
                    ps.setInt(7, item.getPaymentCount6m());
                    ps.setLong(8, item.getMonthlyRevenue());
                    ps.setInt(9, item.getPaymentDelayCount());
                    ps.setLong(10, item.getPrevMonthlyRevenue());
                    ps.setInt(11, item.getPurchaseCycle());
                    ps.setString(12, Boolean.TRUE.equals(item.getVipPrevMonth()) ? "Y" : "N");
                    ps.setFloat(13, item.getAvgOrderVal());
                })
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<FeatureUsage> usageWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<FeatureUsage>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO feature_usage (
                            member_id, feature_base_date,
                            total_usage_amount, avg_daily_usage, max_usage_amount,
                            usage_peak_hour, premium_service_count,
                            last_activity_date, usage_active_days_30d
                        ) VALUES (
                            :memberId, :featureBaseDate,
                            :totalUsageAmount, :avgDailyUsage, :maxUsageAmount,
                            :usagePeakHour, :premiumServiceCount,
                            :lastActivityDate, :usageActiveDays30d
                        )
                        ON DUPLICATE KEY UPDATE
                            total_usage_amount    = VALUES(total_usage_amount),
                            avg_daily_usage       = VALUES(avg_daily_usage),
                            max_usage_amount      = VALUES(max_usage_amount),
                            usage_peak_hour       = VALUES(usage_peak_hour),
                            premium_service_count = VALUES(premium_service_count),
                            last_activity_date    = VALUES(last_activity_date),
                            usage_active_days_30d = VALUES(usage_active_days_30d)
                        """)
                .beanMapped()
                .build();
    }
}