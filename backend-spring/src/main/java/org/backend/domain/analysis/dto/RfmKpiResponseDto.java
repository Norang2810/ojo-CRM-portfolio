package org.backend.domain.analysis.dto;

import java.math.BigDecimal;

public record RfmKpiResponseDto(
    String baseMonth,
    BigDecimal crr,
    String crrStatus,
    BigDecimal churnRate,
    String churnStatus,
    BigDecimal nrr,
    String nrrStatus
) {}
