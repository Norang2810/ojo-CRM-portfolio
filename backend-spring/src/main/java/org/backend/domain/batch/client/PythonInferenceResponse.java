package org.backend.domain.batch.client;

public record PythonInferenceResponse(
        String status,
        String batchId,
        int targetCount,
        int successCount,
        int failureCount,
        String modelVersion,
        String snapshotVersion,
        String dataAsOf,
        boolean alreadyCompleted
) {
}
