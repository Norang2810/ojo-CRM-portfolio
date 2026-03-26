package org.backend.domain.advice.dto;
import java.util.List;
public record AdviceCategoryRatioResponse(
        Long totalCount,
        List<AdviceCategoryRatioItem> items
) {}