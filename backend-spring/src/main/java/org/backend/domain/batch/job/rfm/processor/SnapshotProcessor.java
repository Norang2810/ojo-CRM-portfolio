package org.backend.domain.batch.job.rfm.processor;

import lombok.RequiredArgsConstructor;
import org.backend.domain.analysis.entity.Analysis;
import org.backend.domain.analysis.entity.Invoice;
import org.backend.domain.batch.dto.SnapshotWrapper;
import org.backend.domain.batch.entity.SnapshotBilling;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SnapshotProcessor implements ItemProcessor<SnapshotWrapper, SnapshotBilling> {

    @Override
    public SnapshotBilling process(SnapshotWrapper wrapper){
        Invoice invoice = wrapper.invoice(); // 기준 달 청구서
        SnapshotBilling last = wrapper.lastSnapshot(); // 직전 달 데이터
        Analysis analysis = wrapper.analysis();// 기준 달 분석치

        // 연체 월수 계산 (현재 달 기준 연체된 달)
        Long overdueMonths = invoice.getOverdueAmount() > 0
                            ? (last != null ? last.getOverdueMonths() + 1: 1L)
                            : 0L;
        // 실제 납부된 금액 - 현금 주의(실제 들어온 돈) vs 발생 주의(청구한 금액, 미납은 채권으로 관리)
        Long monthRevenue = invoice.getBilledAmount() - invoice.getOverdueAmount();
        // 고객 누적 매출 계산 = 지난달 누적 + 이번달 매출
        Long totalRevenue = (last != null ? last.getTotalRevenue() : 0L) + monthRevenue;

        String grade = analysis.getType();

        return SnapshotBilling.builder()
                .member(invoice.getMember())
                .baseMonth(invoice.getBaseMonth())
                .billedAmount(invoice.getBilledAmount())
                .overdueAmount(invoice.getOverdueAmount())
                .overdueMonths(overdueMonths)
                .grade(grade) // 해당 월의 analysis.type와 동일
                .monthRevenue(monthRevenue)
                .totalRevenue(totalRevenue)
                .build();
    }
}
