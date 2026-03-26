package org.backend.domain.analysis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardKpiResponseDto {

    @JsonProperty("churn_rate")
    private BigDecimal churnRate;

    @JsonProperty("crr")
    private BigDecimal crr;

    @JsonProperty("nrr")
    private BigDecimal nrr;

    @JsonProperty("base_month")
    private String baseMonth;

    @JsonProperty("kpi_id")
    private Long kpiId;
}
