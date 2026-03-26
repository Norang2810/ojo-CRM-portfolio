package org.backend.domain.batch.job.memberfeature.processor;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.backend.domain.advice.entity.Advice;
import org.backend.domain.batch.entity.ConsultationBasics;
import org.backend.domain.member.entity.Member;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@StepScope
public class ConsultationProcessor implements ItemProcessor<List<Member>, List<ConsultationBasics>> {

    @PersistenceContext
    private EntityManager em;

    private final LocalDate featureBaseDate;

    public ConsultationProcessor(
            @Value("#{jobParameters['featureBaseDate']}") String baseDateStr) {
        this.featureBaseDate = (baseDateStr != null) ? LocalDate.parse(baseDateStr) : LocalDate.now();
    }

    @Override
    public List<ConsultationBasics> process(List<Member> members) {
        LocalDate targetDate = this.featureBaseDate;
        //  targetDate 당일 23:59:59 까지의 상담만 포함 — 배치 기준일 이후 데이터 오염 방지
        LocalDateTime targetLimit = targetDate.atTime(23, 59, 59);

        List<Long> memberIds = members.stream().map(Member::getId).collect(Collectors.toList());


        List<Advice> allAdvices = em.createQuery(
                        "SELECT a FROM Advice a JOIN FETCH a.category " +
                                "WHERE a.member.id IN :memberIds AND a.createdAt <= :targetLimit",
                        Advice.class)
                .setParameter("memberIds", memberIds)
                .setParameter("targetLimit", targetLimit)
                .getResultList();

        Map<Long, List<Advice>> adviceByMember = allAdvices.stream()
                .collect(Collectors.groupingBy(a -> a.getMember().getId()));


        return members.stream()
                .map(member -> {
                    ConsultationBasics basics = createNew(member.getId(), targetDate);
                    List<Advice> adviceList = adviceByMember.getOrDefault(member.getId(), List.of());
                    updateBasicsFields(basics, adviceList, targetDate, member);
                    return basics;
                })
                .collect(Collectors.toList());
    }

    private ConsultationBasics createNew(Long memberId, LocalDate targetDate) {
        ConsultationBasics basics = new ConsultationBasics();
        basics.setMemberId(memberId);
        basics.setFeatureBaseDate(targetDate);
        return basics;
    }

    private void updateBasicsFields(ConsultationBasics basics, List<Advice> adviceList,
                                    LocalDate targetDate, Member member) {
        if (adviceList.isEmpty()) {
            basics.setTotalConsultCount(0);
            basics.setLast7dConsultCount(0);
            basics.setLast30dConsultCount(0);
            basics.setAvgMonthlyConsultCount(0f);
            basics.setLastConsultDate(null);
            basics.setTopConsultCategory("None");
            basics.setTotalComplaintCount(0);
            basics.setLastConsultDaysAgo(null);
            basics.setNightConsultCount(0);
            basics.setWeekendConsultCount(0);
            return;
        }

        LocalDateTime sevenDaysAgo  = targetDate.minusDays(7).atStartOfDay();
        LocalDateTime thirtyDaysAgo = targetDate.minusDays(30).atStartOfDay();

        int  totalCount = adviceList.size();
        long last7d     = adviceList.stream().filter(a -> a.getCreatedAt().isAfter(sevenDaysAgo)).count();
        long last30d    = adviceList.stream().filter(a -> a.getCreatedAt().isAfter(thirtyDaysAgo)).count();

        LocalDateTime lastDateTime = adviceList.stream()
                .map(Advice::getCreatedAt).max(LocalDateTime::compareTo).orElse(null);
        LocalDate lastDate = (lastDateTime != null) ? lastDateTime.toLocalDate() : null;
        int daysAgo = (lastDate != null) ? (int) ChronoUnit.DAYS.between(lastDate, targetDate) : 999;

        // 야간 상담 (22시 ~ 06시)
        int nightCount = (int) adviceList.stream()
                .filter(a -> a.getCreatedAt().getHour() >= 22 || a.getCreatedAt().getHour() < 6)
                .count();

        // 주말 상담
        int weekendCount = (int) adviceList.stream()
                .filter(a -> {
                    DayOfWeek dow = a.getCreatedAt().getDayOfWeek();
                    return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
                }).count();

        // 가장 많이 상담한 카테고리
        String topCategory = adviceList.stream()
                .filter(a -> a.getCategory() != null)
                .collect(Collectors.groupingBy(a -> a.getCategory().getCategoryName(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");

        // 장애, 해지, 환불, 정정, 연체, 미납 키워드 포함 상담을 불만 건수로 집계
        int complaintCount = (int) adviceList.stream()
                .filter(a -> {
                    if (a.getCategory() == null || a.getCategory().getCategoryName() == null) return false;
                    String cat = a.getCategory().getCategoryName();
                    return cat.contains("장애") || cat.contains("품질") || cat.contains("해지") ||
                            cat.contains("환불") || cat.contains("정정") || cat.contains("연체") ||
                            cat.contains("미납");
                }).count();

        basics.setTotalConsultCount(totalCount);
        basics.setLast7dConsultCount((int) last7d);
        basics.setLast30dConsultCount((int) last30d);
        long lifetimeMonths = ChronoUnit.MONTHS.between(member.getCreatedAt().toLocalDate(), targetDate);
        basics.setAvgMonthlyConsultCount(lifetimeMonths > 0 ? totalCount / (float) lifetimeMonths : totalCount);
        basics.setLastConsultDate(lastDate);
        basics.setTopConsultCategory(topCategory);
        basics.setTotalComplaintCount(complaintCount);
        basics.setLastConsultDaysAgo(daysAgo);
        basics.setNightConsultCount(nightCount);
        basics.setWeekendConsultCount(weekendCount);
    }
}