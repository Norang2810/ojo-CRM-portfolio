package org.backend.domain.batch.client;

public record PythonInferenceRequest(
        String batchId,
        String featureBaseAt,
        boolean fullAnalysis,
        boolean retryFailures
) {
}
