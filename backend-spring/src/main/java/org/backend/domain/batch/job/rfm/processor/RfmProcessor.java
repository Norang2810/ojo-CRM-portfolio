package org.backend.domain.batch.job.rfm.processor;

import org.backend.domain.analysis.entity.Rfm;
import org.backend.domain.batch.entity.Monetary;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RfmProcessor implements ItemProcessor<Monetary, Rfm> {
    @Override
    public Rfm process(Monetary monetary){

        if (monetary.getLastPaymentDate() == null) {
            return null;
        }

        Integer frequency = monetary.getPaymentCount6m() != null ? monetary.getPaymentCount6m() : 0;
        Long revenue = monetary.getTotalRevenue() != null ? monetary.getTotalRevenue() : 0L;

        return Rfm.builder()
            .memberId(monetary.getMemberId())
            .recency(monetary.getLastPaymentDate().atStartOfDay())
            .frequency(frequency)
            .monetary(revenue)
            .updatedAt(LocalDateTime.now())
            .build();
    }
}
