package org.backend.domain.batch.client;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Slf4j
@Component
public class PythonInferenceClient {

    private static final Duration[] RETRY_DELAYS = {
            Duration.ZERO,
            Duration.ofSeconds(2),
            Duration.ofSeconds(10)
    };

    private final RestClient restClient;
    private final String inferencePath;
    private final String churnCacheWarmPath;
    private final MeterRegistry meterRegistry;

    public PythonInferenceClient(
            RestClient.Builder restClientBuilder,
            MeterRegistry meterRegistry,
            @Value("${analytics.url}") String analyticsUrl,
            @Value("${analytics.python.inference-path}") String inferencePath,
            @Value("${analytics.python.churn-cache-warm-path:/internal/v1/churn-cache/warm}") String churnCacheWarmPath,
            @Value("${analytics.python.connect-timeout:5s}") Duration connectTimeout,
            @Value("${analytics.python.read-timeout:30m}") Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = restClientBuilder
                .baseUrl(analyticsUrl)
                .requestFactory(requestFactory)
                .build();
        this.inferencePath = inferencePath;
        this.churnCacheWarmPath = churnCacheWarmPath;
        this.meterRegistry = meterRegistry;
    }

    public PythonInferenceResponse infer(PythonInferenceRequest request, int expectedTargetCount) {
        RuntimeException lastFailure = null;

        for (int attempt = 0; attempt < RETRY_DELAYS.length; attempt++) {
            sleep(RETRY_DELAYS[attempt]);
            Timer.Sample sample = Timer.start(meterRegistry);
            try {
                PythonInferenceResponse response = restClient.post()
                        .uri(inferencePath)
                        .body(request)
                        .retrieve()
                        .body(PythonInferenceResponse.class);

                if (response == null || !"completed".equalsIgnoreCase(response.status())) {
                    throw new IllegalStateException("Python inference returned an incomplete response");
                }
                if (response.targetCount() != expectedTargetCount) {
                    throw new IllegalStateException(
                            "Inference target count mismatch: expected=%d, actual=%d"
                                    .formatted(expectedTargetCount, response.targetCount())
                    );
                }
                if (response.successCount() + response.failureCount() != response.targetCount()) {
                    throw new IllegalStateException("Inference success/failure counts do not match the target count");
                }

                sample.stop(Timer.builder("analytics.inference.duration")
                        .tag("result", "success")
                        .register(meterRegistry));
                meterRegistry.counter("analytics.inference.success").increment(response.successCount());
                meterRegistry.counter("analytics.inference.failure").increment(response.failureCount());
                return response;
            } catch (RuntimeException failure) {
                lastFailure = failure;
                sample.stop(Timer.builder("analytics.inference.duration")
                        .tag("result", "failure")
                        .register(meterRegistry));
                log.warn("Python inference attempt {}/{} failed - batchId={}",
                        attempt + 1, RETRY_DELAYS.length, request.batchId(), failure);
            }
        }

        throw new IllegalStateException(
                "Python inference failed after %d idempotent attempts".formatted(RETRY_DELAYS.length),
                lastFailure
        );
    }

    public void runLegacyFullAnalysis() {
        restClient.get()
                .uri("/api/analysis/make")
                .retrieve()
                .toBodilessEntity();
    }

    public void warmChurnSummaryCache() {
        restClient.post()
                .uri(churnCacheWarmPath)
                .retrieve()
                .toBodilessEntity();
    }

    private void sleep(Duration delay) {
        if (delay.isZero()) return;
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Python inference retry was interrupted", interrupted);
        }
    }
}
