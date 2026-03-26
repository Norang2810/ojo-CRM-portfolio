package org.backend.domain.batch.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 2. 배치 이력 조회용 목록 응답
@Getter
@Builder
public class BatchHistoryListResponse {
    private int totalCount;
    private List<BatchHistoryItem> batchList;
}