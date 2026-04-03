package org.backend.domain.analysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.domain.analysis.dto.DashboardCardsDto;
import org.backend.domain.analysis.dto.DashboardDailyStatDto;
import org.backend.domain.analysis.dto.DashboardSegmentStatDto;
import org.backend.domain.analysis.dto.DashboardSummaryResponseDto;
import org.backend.domain.analysis.entity.DashboardDailyStats;
import org.backend.domain.analysis.entity.DashboardSummaryStats;
import org.backend.domain.analysis.repository.DashboardDailyStatsRepository;
import org.backend.domain.analysis.repository.DashboardSummaryStatsRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardSummaryStatsRepository dashboardSummaryStatsRepository;
    private final DashboardDailyStatsRepository dashboardDailyStatsRepository;

    @Cacheable(value = "dashboardCache", key = "'summary'")
    public DashboardSummaryResponseDto getDashboardSummary() {
        log.info("대시보드 요약 조회 - 선계산 테이블 조회");

        DashboardSummaryStats summary = dashboardSummaryStatsRepository.findTopByOrderByStatDateDesc()
                .orElseThrow(() -> new IllegalStateException("대시보드 선계산 데이터가 존재하지 않습니다."));

        LocalDate endDate = summary.getStatDate();
        LocalDate startDate = endDate.minusDays(6);

        List<DashboardDailyStatDto> dailyStats = dashboardDailyStatsRepository
                .findByStatDateBetweenOrderByStatDateAsc(startDate, endDate)
                .stream()
                .map(daily -> new DashboardDailyStatDto(
                        daily.getStatDate().toString(),
                        daily.getNewCustomers(),
                        daily.getChurnedCustomers(),
                        daily.getActiveCustomers()
                ))
                .toList();

        DashboardCardsDto cards = DashboardCardsDto.builder()
                .currentCustomers(new DashboardCardsDto.CardStat(
                        summary.getCurrentCustomers(),
                        summary.getCurrentCustomersChangeRate()
                ))
                .newActiveCustomers(new DashboardCardsDto.CardStat(
                        summary.getNewActiveCustomers(),
                        summary.getNewActiveCustomersChangeRate()
                ))
                .newCustomers(new DashboardCardsDto.CardStat(
                        summary.getNewCustomers(),
                        summary.getNewCustomersChangeRate()
                ))
                .atRiskCustomers(new DashboardCardsDto.CardStat(
                        summary.getAtRiskCustomers(),
                        summary.getAtRiskCustomersChangeRate()
                ))
                .build();

        DashboardSegmentStatDto segments = DashboardSegmentStatDto.builder()
                .vip(summary.getVipCount())
                .potentialVip(summary.getPotentialVipCount())
                .general(summary.getGeneralCount())
                .atRisk(summary.getAtRiskCount())
                .churned(summary.getChurnedCount())
                .build();

        return DashboardSummaryResponseDto.builder()
                .cards(cards)
                .dailyStats(dailyStats)
                .segments(segments)
                .build();
    }
}