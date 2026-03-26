package org.backend.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDailyStatDto {
    private String date;
    private long newCustomers;
    private long churnedCustomers;
    private long activeCustomers;
}
