package org.backend.domain.batch.job.memberfeature.processor;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.backend.domain.batch.entity.FeatureUsage;
import org.backend.domain.member.entity.Member;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@StepScope
public class UsageProcessor implements ItemProcessor<List<Member>, List<FeatureUsage>> {


    @PersistenceContext
    private EntityManager em;

    private final LocalDate featureBaseDate;

    public UsageProcessor(
            @Value("#{jobParameters['featureBaseDate']}") String baseDateStr) {
        this.featureBaseDate = (baseDateStr != null) ? LocalDate.parse(baseDateStr) : LocalDate.now();
    }

    @Override
    public List<FeatureUsage> process(List<Member> members) {
        LocalDate targetDate = this.featureBaseDate;
        LocalDate thirtyDaysAgo = targetDate.minusDays(30);
        LocalDateTime targetLimit = targetDate.atTime(23, 59, 59);

        List<Long> memberIds = members.stream()
                .map(Member::getId)
                .collect(Collectors.toList());

        // 전체 사용량 통계
        Map<Long, Object[]> usageStatsMap = em.createQuery(
                        "SELECT d.member.id, SUM(d.usageAmount), AVG(d.usageAmount), MAX(d.usageAmount), MAX(d.usageDate) " +
                                "FROM DataUsage d WHERE d.member.id IN :memberIds AND d.usageDate <= :targetDate " +
                                "GROUP BY d.member.id",
                        Object[].class)
                .setParameter("memberIds", memberIds)
                .setParameter("targetDate", targetDate)
                .getResultStream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> row
                ));

        // 활동 일수 (최근 30일)
        Map<Long, Integer> activeDaysMap = em.createQuery(
                        "SELECT d.member.id, COUNT(DISTINCT d.usageDate) FROM DataUsage d " +
                                "WHERE d.member.id IN :memberIds AND d.usageDate BETWEEN :startDate AND :endDate " +
                                "GROUP BY d.member.id",
                        Object[].class)
                .setParameter("memberIds", memberIds)
                .setParameter("startDate", thirtyDaysAgo)
                .setParameter("endDate", targetDate)
                .getResultStream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()
                ));

        // 프리미엄 서비스 카운트
        Map<Long, Integer> premiumCountMap = em.createQuery(
                        "SELECT i.member.id, COUNT(id.id) FROM InvoiceDetail id JOIN id.invoice i " +
                                "WHERE i.member.id IN :memberIds AND id.productType NOT IN ('BASE') " +
                                "AND i.createdAt <= :targetLimit " +
                                "GROUP BY i.member.id",
                        Object[].class)
                .setParameter("memberIds", memberIds)
                .setParameter("targetLimit", targetLimit)
                .getResultStream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()
                ));

        // 피크 타임 — 멤버별로 count 가장 큰 usageTime 선택
        Map<Long, Integer> peakHourMap = em.createQuery(
                        "SELECT d.member.id, d.usageTime, COUNT(d.id) as cnt FROM DataUsage d " +
                                "WHERE d.member.id IN :memberIds AND d.usageDate <= :targetDate " +
                                "GROUP BY d.member.id, d.usageTime",
                        Object[].class)
                .setParameter("memberIds", memberIds)
                .setParameter("targetDate", targetDate)
                .getResultStream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.collectingAndThen(
                                Collectors.maxBy(java.util.Comparator.comparingLong(row -> ((Number) row[2]).longValue())),
                                opt -> opt.map(row -> ((Number) row[1]).intValue()).orElse(0)
                        )
                ));

        return members.stream()
                .map(member -> buildFeatureUsage(
                        member, targetDate,
                        usageStatsMap, activeDaysMap, premiumCountMap, peakHourMap))
                .collect(Collectors.toList());
    }

    private FeatureUsage buildFeatureUsage(
            Member member, LocalDate targetDate,
            Map<Long, Object[]> usageStatsMap,
            Map<Long, Integer> activeDaysMap,
            Map<Long, Integer> premiumCountMap,
            Map<Long, Integer> peakHourMap) {

        Long memberId = member.getId();

        Object[] stats = usageStatsMap.get(memberId);
        long totalUsage  = (stats != null && stats[1] != null) ? ((Number) stats[1]).longValue() : 0L;
        double avgUsage  = (stats != null && stats[2] != null) ? ((Number) stats[2]).doubleValue() : 0.0;
        long maxUsage    = (stats != null && stats[3] != null) ? ((Number) stats[3]).longValue() : 0L;
        LocalDate lastActivity = (stats != null && stats[4] != null) ? (LocalDate) stats[4] : null;

        int activeDays   = activeDaysMap.getOrDefault(memberId, 0);
        int premiumCount = premiumCountMap.getOrDefault(memberId, 0);
        int peakHour     = peakHourMap.getOrDefault(memberId, 0);

        return FeatureUsage.builder()
                .memberId(memberId)
                .featureBaseDate(targetDate)
                .totalUsageAmount(totalUsage)
                .avgDailyUsage((float) avgUsage)
                .maxUsageAmount(maxUsage)
                .usagePeakHour(peakHour)
                .premiumServiceCount(premiumCount)
                .lastActivityDate(lastActivity)
                .usageActiveDays30d(activeDays)
                .build();
    }
}