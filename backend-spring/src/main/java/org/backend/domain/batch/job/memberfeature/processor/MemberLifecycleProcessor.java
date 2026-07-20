package org.backend.domain.batch.job.memberfeature.processor;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.backend.domain.batch.entity.Lifecycle;
import org.backend.domain.member.entity.Member;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@StepScope
public class MemberLifecycleProcessor implements ItemProcessor<List<Member>, List<Lifecycle>> {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    @PersistenceContext
    private EntityManager em;

    private final LocalDate featureBaseDate;
    private final LocalDateTime featureBaseAt;
    private final String batchId;

    public MemberLifecycleProcessor(
            @Value("#{jobParameters['featureBaseDate']}") String baseDateStr,
            @Value("#{jobParameters['featureBaseAt']}") String featureBaseAtStr,
            @Value("#{jobParameters['batchId']}") String batchId) {
        this.featureBaseDate = (baseDateStr != null) ? LocalDate.parse(baseDateStr) : LocalDate.now();
        this.featureBaseAt = featureBaseAtStr != null
                ? LocalDateTime.parse(featureBaseAtStr)
                : this.featureBaseDate.plusDays(1).atStartOfDay();
        this.batchId = batchId;
    }

    @Override
    public List<Lifecycle> process(List<Member> members) {
        LocalDate today = this.featureBaseDate;

        List<Long> memberIds = members.stream()
                .map(Member::getId)
                .collect(Collectors.toList());

        // 청크의 lastActivityDate 조회
        Map<Long, LocalDate> lastActivityMap = em.createQuery(
                        "SELECT d.member.id, MAX(d.usageDate) FROM DataUsage d " +
                                "WHERE d.member.id IN :memberIds AND d.usageDate <= :today " +
                                "AND d.createdAt < :featureBaseAt " +
                                "GROUP BY d.member.id",
                        Object[].class)
                .setParameter("memberIds", memberIds)
                .setParameter("today", today)
                .setParameter("featureBaseAt", featureBaseAt)
                .getResultStream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (LocalDate) row[1]
                ));

        return members.stream()
                .map(member -> buildLifecycle(member, today, lastActivityMap))
                .collect(Collectors.toList());
    }



    private Lifecycle buildLifecycle(Member member, LocalDate today, Map<Long, LocalDate> lastActivityMap) {
        LocalDate signupDate = member.getCreatedAt()
                .atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(BUSINESS_ZONE)
                .toLocalDate();
        int lifetimeDays = (int) ChronoUnit.DAYS.between(signupDate, today);

        LocalDate lastActivityDate = lastActivityMap.get(member.getId());
        int daysSinceLastActivity = (lastActivityDate != null)
                ? (int) ChronoUnit.DAYS.between(lastActivityDate, today)
                : lifetimeDays;

        // 휴면/해지 상태면 약정만료일 0
        String status = member.getStatus();
        boolean isInactive = "DORMANT".equalsIgnoreCase(status) || "TERMINATED".equalsIgnoreCase(status);

        int totalContractDays = 1825;
        int contractEndDaysLeft = isInactive ? 0 : Math.max(0, totalContractDays - lifetimeDays);

        return Lifecycle.builder()
                .memberId(member.getId())
                .featureBaseDate(today)
                .featureBaseAt(featureBaseAt)
                .batchId(batchId)
                .signupDate(signupDate)
                .memberLifetimeDays(lifetimeDays)
                .isNewCustomerFlag(lifetimeDays <= 30)
                .isDormantFlag("DORMANT".equalsIgnoreCase(status))
                .isTerminatedFlag("TERMINATED".equalsIgnoreCase(status))
                .daysSinceLastActivity(daysSinceLastActivity)
                .contractEndDaysLeft(contractEndDaysLeft)
                .build();
    }
}



