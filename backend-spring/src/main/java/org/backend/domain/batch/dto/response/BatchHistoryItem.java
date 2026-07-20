package org.backend.domain.batch.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BatchHistoryItem {
    private String batchId;
    private String featureBaseDate;
    private String batchStatus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String runType;
    private LocalDateTime windowStartAt;
    private LocalDateTime windowEndAt;
    private long targetCount;
    private long successCount;
    private long failureCount;
    private String modelVersion;
    private LocalDateTime lastSuccessfulCompletion;
}
