package org.backend.config.batch;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AnalyticsClockConfig {

    @Bean
    public Clock analyticsClock() {
        return Clock.systemUTC();
    }
}
