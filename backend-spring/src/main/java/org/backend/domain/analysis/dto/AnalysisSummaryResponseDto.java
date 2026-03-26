package org.backend.domain.analysis.dto;

public record AnalysisSummaryResponseDto (
    // analysis
    Long memberId,
    String rfmType,
    Integer rfmScore,
    Long ltv,
    String lifecycleStage
){}
