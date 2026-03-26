package org.backend.domain.analysis.service;

import lombok.RequiredArgsConstructor;
import org.backend.domain.analysis.dto.DashboardCardsDto;
import org.backend.domain.analysis.dto.DashboardDailyStatDto;
import org.backend.domain.analysis.dto.DashboardSegmentStatDto;
import org.backend.domain.analysis.dto.DashboardSummaryResponseDto;
import org.backend.domain.analysis.repository.DashboardRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository dashboardRepository;

    public DashboardSummaryResponseDto getDashboardSummary() {
        LocalDate today = LocalDate.now();
        LocalDate startOfThisMonth = today.withDayOfMonth(1);
        
        // This week and last week
        LocalDate startOfThisWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate startOfLastWeek = startOfThisWeek.minusWeeks(1);
        LocalDate endOfLastWeek = startOfThisWeek.minusDays(1);

        String todayStr = today.plusDays(1).toString(); // end boundary exclusive
        String monthStartStr = startOfThisMonth.toString();
        
        String thisWeekStartStr = startOfThisWeek.toString();
        String lastWeekStartStr = startOfLastWeek.toString();
        String lastWeekEndStr = endOfLastWeek.toString();

        // Current overall vs last week overall
        long currentTotal = dashboardRepository.countCurrentCustomers(todayStr);
        long lastWeekTotal = dashboardRepository.countCurrentCustomers(lastWeekEndStr);
        
        // New Active Customers
        long newActiveThisMonth = dashboardRepository.countNewActiveCustomers(monthStartStr, todayStr);
        long newActiveThisWeek = dashboardRepository.countNewActiveCustomers(thisWeekStartStr, todayStr);
        long newActiveLastWeek = dashboardRepository.countNewActiveCustomers(lastWeekStartStr, lastWeekEndStr);
        
        // New Customers
        long newThisMonth = dashboardRepository.countNewCustomers(monthStartStr, todayStr);
        long newThisWeek = dashboardRepository.countNewCustomers(thisWeekStartStr, todayStr);
        long newLastWeek = dashboardRepository.countNewCustomers(lastWeekStartStr, lastWeekEndStr);

        // At-risk 
        long riskThisMonth = dashboardRepository.countAtRiskCustomers(monthStartStr, todayStr);
        long riskThisWeek = dashboardRepository.countAtRiskCustomers(thisWeekStartStr, todayStr);
        long riskLastWeek = dashboardRepository.countAtRiskCustomers(lastWeekStartStr, lastWeekEndStr);

        DashboardCardsDto cards = DashboardCardsDto.builder()
                .currentCustomers(new DashboardCardsDto.CardStat(currentTotal, calculateChange(currentTotal, lastWeekTotal)))
                .newActiveCustomers(new DashboardCardsDto.CardStat(newActiveThisMonth, calculateChange(newActiveThisWeek, newActiveLastWeek)))
                .newCustomers(new DashboardCardsDto.CardStat(newThisMonth, calculateChange(newThisWeek, newLastWeek)))
                .atRiskCustomers(new DashboardCardsDto.CardStat(riskThisMonth, calculateChange(riskThisWeek, riskLastWeek)))
                .build();

        // Last 7 days daily stats
        LocalDate sevenDaysAgo = today.minusDays(6);
        String sevenDaysAgoStr = sevenDaysAgo.toString();
        
        List<Map<String, Object>> newDailies = dashboardRepository.getDailyNewCustomers(sevenDaysAgoStr, todayStr);
        List<Map<String, Object>> churnedDailies = dashboardRepository.getDailyChurnedCustomers(sevenDaysAgoStr, todayStr);
        List<Map<String, Object>> activeDailies = dashboardRepository.getDailyActiveCustomers(sevenDaysAgoStr, todayStr);

        Map<String, Long> mappedNew = parseDaily(newDailies, "statDate", "newCount");
        Map<String, Long> mappedChurn = parseDaily(churnedDailies, "statDate", "churnedCount");
        Map<String, Long> mappedActive = parseDaily(activeDailies, "statDate", "activeCount");

        List<DashboardDailyStatDto> dailyStats = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i <= 6; i++) {
            String dateStr = sevenDaysAgo.plusDays(i).format(fmt);
            dailyStats.add(new DashboardDailyStatDto(
                    dateStr,
                    mappedNew.getOrDefault(dateStr, 0L),
                    mappedChurn.getOrDefault(dateStr, 0L),
                    mappedActive.getOrDefault(dateStr, 0L)
            ));
        }

        // Segments
        List<Map<String, Object>> segmentsDb = dashboardRepository.getSegmentCounts();
        long vip = 0, potentialVip = 0, general = 0, atRisk = 0, churned = 0;
        
        for (Map<String, Object> row : segmentsDb) {
            String type = (String) row.get("type");
            long cnt = ((Number) row.get("cnt")).longValue();
            if (type == null) type = "COMMON";
            
            switch (type) {
                case "VIP": vip += cnt; break;
                case "LOYAL": potentialVip += cnt; break;
                case "COMMON": general += cnt; break;
                case "RISK": 
                case "SLEEP": atRisk += cnt; break;
                case "LOST": churned += cnt; break;
            }
        }

        DashboardSegmentStatDto segments = new DashboardSegmentStatDto(vip, potentialVip, general, atRisk, churned);

        return new DashboardSummaryResponseDto(cards, dailyStats, segments);
    }

    private double calculateChange(long current, long previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return Math.round((((double) current - previous) / previous) * 1000.0) / 10.0;
    }

    private Map<String, Long> parseDaily(List<Map<String, Object>> list, String dateKey, String countKey) {
        Map<String, Long> map = new HashMap<>();
        for (Map<String, Object> row : list) {
            String date = row.get(dateKey).toString();
            Long count = ((Number) row.get(countKey)).longValue();
            map.put(date, count);
        }
        return map;
    }
}
