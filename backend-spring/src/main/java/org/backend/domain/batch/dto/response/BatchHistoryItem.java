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
}