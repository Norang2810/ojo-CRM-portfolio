package org.backend.domain.batch.pipeline;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AnalyticsRunContext(
        String batchId,
        AnalyticsRunType runType,
        LocalDate featureBaseDate,
        LocalDateTime featureBaseAt,
        LocalDateTime windowStartAt,
        LocalDateTime windowEndAt
) {
}
