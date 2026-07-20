package org.backend.config.batch;

import io.micrometer.core.instrument.MeterRegistry;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutorListener;
import net.javacrumbs.shedlock.micrometer.MicrometerLockingTaskExecutorListener;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT6H")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build()
        );
    }

    @Bean
    public LockingTaskExecutorListener lockingTaskExecutorListener(MeterRegistry meterRegistry) {
        MicrometerLockingTaskExecutorListener listener =
                new MicrometerLockingTaskExecutorListener(meterRegistry);
        listener.registerMetricsFor("analytics-feature-pipeline", "dashboard-refresh");
        return listener;
    }
}
