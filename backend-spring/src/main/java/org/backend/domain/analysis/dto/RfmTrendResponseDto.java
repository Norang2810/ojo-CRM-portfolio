package org.backend.domain.analysis.dto;

public interface RfmTrendResponseDto {
    String getBaseMonth();
    String getType();
    Double getAvgMonetary();
    Long getCount();
}

