package org.backend.domain.advice.dto;

import java.util.List;

public record AdviceOutboundResponse(
        long totalAttempt,
        List<AdvicePromotionStatItem> promotionStats
) {
}