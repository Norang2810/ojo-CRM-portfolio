package org.backend.domain.batch.job.rfm.reader;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.backend.domain.batch.dto.SnapshotWrapper;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class SnapshotReaderConfig {

    private final EntityManagerFactory emf;

    @Bean
    @StepScope
    public JpaPagingItemReader<SnapshotWrapper> snapshotReader(
            @Value("#{jobParameters['baseMonth']}") String baseMonth) {
        String base = baseMonth.replace("-", "");
        String last = calculateLastMonth(baseMonth).replace("-", "");

        return new JpaPagingItemReaderBuilder<SnapshotWrapper>()
                .name("snapshotReader")
                .entityManagerFactory(emf)
                .queryString("SELECT new org.backend.domain.batch.dto.SnapshotWrapper(i, s, a) " +
                        " FROM Invoice i " +
                        " LEFT JOIN SnapshotBilling s ON i.member = s.member " +
                        " AND s.baseMonth = :last " +
                        " JOIN Analysis a ON i.member.id = a.member.id " +
                        " AND FUNCTION('DATE_FORMAT', a.createdAt, '%Y%m') = :base " +
                        " WHERE i.baseMonth = :base")
                .parameterValues(Map.of(
                    "base", base,
                    "last", last
                ))
                .pageSize(1000)
                .saveState(false)
                .build();
    }

    public String calculateLastMonth(String baseMonth){
        String pattern = baseMonth.contains("-") ? "yyyy-MM" : "yyyyMM";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        YearMonth ym = YearMonth.parse(baseMonth, formatter);
        return ym.minusMonths(1).format(formatter);
    }
}
