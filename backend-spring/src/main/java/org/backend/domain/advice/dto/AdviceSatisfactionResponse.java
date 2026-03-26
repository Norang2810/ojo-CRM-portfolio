package org.backend.domain.advice.dto;

import java.util.List;

public record AdviceSatisfactionResponse(
    Double averageScore,
    Long totalCount,
    List<SatisfactionScoreCount> scoreDistribution
) {
    public record SatisfactionScoreCount(long score, long count) {}
}
