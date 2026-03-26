package org.backend.domain.advice.dto;
public record AdviceCategoryRatioItem(
        Long categoryId,
        String categoryName,
        Long count,
        Double ratio
)
{}