package org.backend.domain.batch.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class AnalyticsPipelineMetrics {

    private final AtomicLong lastSuccessfulCompletion = new AtomicLong(0);
    private final AtomicLong snapshotLagSeconds = new AtomicLong(0);
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public AnalyticsPipelineMetrics(MeterRegistry meterRegistry, Clock clock) {
        this.meterRegistry = meterRegistry;
        this.clock = clock;
        Gauge.builder("analytics.last_successful_completion", lastSuccessfulCompletion, AtomicLong::get)
                .description("Epoch seconds of the last successful analytics publish")
                .register(meterRegistry);
        Gauge.builder("analytics.snapshot.lag.seconds", snapshotLagSeconds, AtomicLong::get)
                .description("Lag between now and the active snapshot data-as-of timestamp")
                .register(meterRegistry);
    }

    public void recordPublish(LocalDateTime dataAsOf, long featureRows) {
        long now = clock.instant().getEpochSecond();
        long asOf = dataAsOf.toEpochSecond(ZoneOffset.UTC);
        lastSuccessfulCompletion.set(now);
        snapshotLagSeconds.set(Math.max(0, now - asOf));
        for (String featureType : new String[]{"consultation", "lifecycle", "monetary", "usage"}) {
            meterRegistry.counter("analytics.feature.rows", "featureType", featureType)
                    .increment(featureRows);
        }
    }
}
