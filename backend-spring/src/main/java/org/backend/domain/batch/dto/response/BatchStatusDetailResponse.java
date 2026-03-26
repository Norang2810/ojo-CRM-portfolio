package org.backend.domain.batch.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 1. 배치 상태 상세 조회용 (단건)
@Getter
@Builder
public class BatchStatusDetailResponse {
    private String batchId;           // Execution ID 등을 활용한 식별자
    private String batchStatus;       // RUNNING, COMPLETED, FAILED 등
    private String featureBaseDate;   // JobParameter에서 추출
    private long totalTargetCount;    // 전체 대상 건수 (Step의 readCount 기반)
    private long processedCount;      // 현재까지 처리된 건수
    private long successCount;        // 성공 건수 (writeCount)
    private long failCount;           // 실패 건수 (Process/Write Skip 등 포함)
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}