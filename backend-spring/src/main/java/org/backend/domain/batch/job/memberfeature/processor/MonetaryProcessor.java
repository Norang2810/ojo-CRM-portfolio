package org.backend.domain.batch.job.memberfeature.processor;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.backend.domain.batch.entity.Monetary;
import org.backend.domain.member.entity.Member;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Component
@StepScope
public class MonetaryProcessor implements ItemProcessor<List<Member>, List<Monetary>> {

    @PersistenceContext
    private EntityManager em;

    private final LocalDate featureBaseDate;

    public MonetaryProcessor(@Value("#{jobParameters['featureBaseDate']}") String baseDateStr) {
        this.featureBaseDate = (baseDateStr != null) ? LocalDate.parse(baseDateStr) : LocalDate.now();
    }

    private <V> Map<Long, V> toSafeMap(java.util.stream.Stream<Object[]> stream,
                                       java.util.function.Function<Object[], V> valueMapper) {
        Map<Long, V> result = new HashMap<>();
        stream.forEach(row -> {
            if (row != null && row[0] != null) {
                V value = valueMapper.apply(row);
                if (value != null) result.put((Long) row[0], value);
            }
        });
        return result;
    }

    @Override
    public List<Monetary> process(List<Member> members) {
        LocalDate targetDate = this.featureBaseDate;
        LocalDateTime targetLimit = targetDate.atTime(23, 59, 59);
        LocalDateTime sixMonthsAgo = targetDate.minusMonths(6).atStartOfDay();
        List<Long> memberIds = members.stream().map(Member::getId).collect(Collectors.toList());

        // 1. 전체 매출 통계 (billedAmount 합계, 평균)
        Map<Long, Object[]> totalStatsMap = toSafeMap(
                em.createQuery("SELECT i.member.id, SUM(i.billedAmount), AVG(i.billedAmount) " +
                                "FROM Invoice i WHERE i.member.id IN :memberIds AND i.createdAt <= :targetLimit " +
                                "GROUP BY i.member.id", Object[].class)
                        .setParameter("memberIds", memberIds).setParameter("targetLimit", targetLimit).getResultStream(),
                row -> row);

        // 2. 최근 6개월 평균 청구액
        Map<Long, Double> sixMonthAvgMap = toSafeMap(
                em.createQuery("SELECT i.member.id, AVG(i.billedAmount) FROM Invoice i " +
                                "WHERE i.member.id IN :memberIds AND i.createdAt BETWEEN :sixMonthsAgo AND :targetLimit " +
                                "GROUP BY i.member.id", Object[].class)
                        .setParameter("memberIds", memberIds).setParameter("sixMonthsAgo", sixMonthsAgo).setParameter("targetLimit", targetLimit).getResultStream(),
                row -> row[1] != null ? ((Number) row[1]).doubleValue() : 0.0);

        // 3. 마지막 결제 정보 (가장 최근 결제액과 날짜)
        Map<Long, Object[]> lastPaymentMap = toSafeMap(
                em.createQuery("SELECT i.member.id, p.paidAmount, p.paidAt " +
                                "FROM Payment p JOIN p.invoice i " +
                                "WHERE i.member.id IN :memberIds AND p.paidAt <= :targetLimit " +
                                "AND p.paidAt = (SELECT MAX(p2.paidAt) FROM Payment p2 JOIN p2.invoice i2 WHERE i2.member.id = i.member.id AND p2.paidAt <= :targetLimit) ", Object[].class)
                        .setParameter("memberIds", memberIds).setParameter("targetLimit", targetLimit).getResultStream(),
                row -> row);

        // 4. 연체 횟수
        Map<Long, Integer> delayCountMap = toSafeMap(
                em.createQuery("SELECT i.member.id, COUNT(i.id) FROM Invoice i " +
                                "WHERE i.member.id IN :memberIds AND i.overdueAmount > 0 AND i.createdAt <= :targetLimit " +
                                "GROUP BY i.member.id", Object[].class)
                        .setParameter("memberIds", memberIds).setParameter("targetLimit", targetLimit).getResultStream(),
                row -> ((Number) row[1]).intValue());

        // 5. 구매 주기
        Map<Long, Object[]> addonStatsMap = toSafeMap(
                em.createQuery("SELECT i.member.id, COUNT(DISTINCT p.id), MIN(p.paidAt), MAX(p.paidAt) " +
                                "FROM Payment p JOIN p.invoice i JOIN i.invoiceDetails id " +
                                "WHERE i.member.id IN :memberIds " +
                                // 핵심 수정: SUBSCRIPTION(정기결제)은 제외하고 ONE_TIME(일회성 구매)만 집계
                                "AND UPPER(id.productType) = 'ONE_TIME' " +
                                "AND p.paidAt <= :targetLimit " +
                                "GROUP BY i.member.id", Object[].class)
                        .setParameter("memberIds", memberIds).setParameter("targetLimit", targetLimit).getResultStream(),
                row -> row);

        // 6. 당월/전월 매출 계산을 위한 준비
        String curMonth = targetDate.getYear() + String.format("%02d", targetDate.getMonthValue());
        String preMonth = targetDate.minusMonths(1).getYear() + String.format("%02d", targetDate.minusMonths(1).getMonthValue());

        Map<Long, Long> currMonthMap = queryMonthlyRevenue(memberIds, curMonth);
        Map<Long, Long> prevMonthMap = queryMonthlyRevenue(memberIds, preMonth);
        Map<Long, Long> payCount6mMap = toSafeMap(
                em.createQuery("SELECT i.member.id, COUNT(p.id) FROM Payment p JOIN p.invoice i " +
                                "WHERE i.member.id IN :memberIds AND p.paidAt BETWEEN :sixMonthsAgo AND :targetLimit " +
                                "GROUP BY i.member.id", Object[].class)
                        .setParameter("memberIds", memberIds).setParameter("sixMonthsAgo", sixMonthsAgo).setParameter("targetLimit", targetLimit).getResultStream(),
                row -> ((Number) row[1]).longValue());

        return members.stream()
                .map(m -> buildMonetary(m, targetDate, totalStatsMap, sixMonthAvgMap, lastPaymentMap, delayCountMap, addonStatsMap, payCount6mMap, currMonthMap, prevMonthMap))
                .collect(Collectors.toList());
    }

    private Map<Long, Long> queryMonthlyRevenue(List<Long> ids, String bm) {
        return toSafeMap(em.createQuery("SELECT i.member.id, SUM(i.billedAmount) FROM Invoice i WHERE i.member.id IN :ids AND i.baseMonth = :bm GROUP BY i.member.id", Object[].class)
                .setParameter("ids", ids).setParameter("bm", bm).getResultStream(), row -> row[1] != null ? ((Number) row[1]).longValue() : 0L);
    }

    private Monetary buildMonetary(Member member, LocalDate targetDate, Map<Long, Object[]> totalStatsMap, Map<Long, Double> sixMonthAvgMap, Map<Long, Object[]> lastPaymentMap, Map<Long, Integer> delayCountMap, Map<Long, Object[]> addonStatsMap, Map<Long, Long> payCount6mMap, Map<Long, Long> currMonthMap, Map<Long, Long> prevMonthMap) {
        Long mid = member.getId();

        // 1. 전체 매출/평균가
        Object[] tRow = totalStatsMap.get(mid);
        long totalRev = (tRow != null && tRow[1] != null) ? ((Number) tRow[1]).longValue() : 0L;
        double avgOrd = (tRow != null && tRow[2] != null) ? ((Number) tRow[2]).doubleValue() : 0.0;

        // 2. 마지막 결제
        Object[] lRow = lastPaymentMap.get(mid);
        long lastAmt = (lRow != null && lRow[1] != null) ? ((Number) lRow[1]).longValue() : 0L;
        LocalDate lastDt = (lRow != null && lRow[2] != null) ? ((LocalDateTime) lRow[2]).toLocalDate() : null;

        // 3. 구매 주기 (0 또는 30만 나오는 문제 해결부)
        int cycle = 0;
        Object[] aRow = addonStatsMap.get(mid);
        if (aRow != null) {
            long count = (aRow[1] != null) ? ((Number) aRow[1]).longValue() : 0L;
            if (count >= 2 && aRow[2] != null && aRow[3] != null) {
                long days = ChronoUnit.DAYS.between(((LocalDateTime) aRow[2]).toLocalDate(), ((LocalDateTime) aRow[3]).toLocalDate());
                cycle = (int) (days / (count - 1));
            }
        }

        long prevMonthRev = prevMonthMap.getOrDefault(mid, 0L);

        return Monetary.builder().memberId(mid).featureBaseDate(targetDate)
                .totalRevenue(totalRev).lastPaymentAmount(lastAmt).lastPaymentDate(lastDt)
                .avgMonthlyBill(sixMonthAvgMap.getOrDefault(mid, 0.0).floatValue())
                .paymentCount6m(payCount6mMap.getOrDefault(mid, 0L).intValue())
                .monthlyRevenue(currMonthMap.getOrDefault(mid, 0L))
                .paymentDelayCount(delayCountMap.getOrDefault(mid, 0))
                .prevMonthlyRevenue(prevMonthRev)
                .purchaseCycle(cycle).avgOrderVal((float) avgOrd)
                .vipPrevMonth(prevMonthRev >= 100000).build();
    }
}