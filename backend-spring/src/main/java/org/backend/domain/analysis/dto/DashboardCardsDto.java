package org.backend.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardCardsDto {
    private CardStat currentCustomers;
    private CardStat newActiveCustomers;
    private CardStat newCustomers;
    private CardStat atRiskCustomers;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardStat {
        private long count;
        private double percentChange; 
    }
}
