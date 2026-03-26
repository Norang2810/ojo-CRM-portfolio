package org.backend.domain.advice.dto;

public interface AdviceCategoryRatioRow {
    Long getCategoryId();
    String getCategoryName();
    Long getCount();
    Long getTotalCount();
    Double getRatio();
}