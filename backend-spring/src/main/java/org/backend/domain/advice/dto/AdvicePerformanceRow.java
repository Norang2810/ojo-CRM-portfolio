package org.backend.domain.advice.dto;

public interface AdvicePerformanceRow{
    Long getAdminId();
    String getAdminName();
    String getRole();
    Long getTotalCount();
    Double getAvgSatisfaction();
    Double getAvgDurationSeconds();
    Long getInboundCount();
    Long getOutboundCount();

    String getMainCategory();
    Long getMainCategoryCount();
}
