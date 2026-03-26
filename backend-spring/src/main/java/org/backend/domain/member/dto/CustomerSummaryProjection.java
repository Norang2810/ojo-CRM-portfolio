package org.backend.domain.member.dto;

import java.time.LocalDateTime;

public interface CustomerSummaryProjection {

    Long getMemberId();
    String getName();
    String getEmail();
    String getPhone();
    LocalDateTime getCreatedAt();

    String getTopConsultCategory();
    Double getLast30dConsultCount();

    String getVipType();

    String getProductName();
}
