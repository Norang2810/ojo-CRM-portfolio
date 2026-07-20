package org.backend.domain.batch.pipeline;

public enum AnalyticsRunStatus {
    BUILDING,
    FEATURE_READY,
    INFERENCING,
    READY,
    READY_WITH_ERRORS,
    FAILED
}
