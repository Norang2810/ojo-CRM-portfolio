package org.backend.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSegmentStatDto {
    private long vip;
    private long potentialVip;
    private long general;
    private long atRisk;
    private long churned;
}
