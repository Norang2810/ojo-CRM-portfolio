package org.backend.domain.analysis.dto;

public record LtvResponseDto(
    // analysis
    Long memberId,
    Long ltv,
    String lifecycleStage
) {}
