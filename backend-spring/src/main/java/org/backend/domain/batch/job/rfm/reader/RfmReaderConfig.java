package org.backend.domain.batch.job.rfm.reader;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.backend.domain.batch.entity.Monetary;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class RfmReaderConfig {

    private final EntityManagerFactory emf;

    @Bean
    @StepScope
    public JpaPagingItemReader<Monetary> rfmReader(
            @Value("#{jobParameters['featureBaseDate']}") String featureBaseDate) {
        JpaPagingItemReaderBuilder<Monetary> builder = new JpaPagingItemReaderBuilder<Monetary>()
                .name("rfmReader")
                .entityManagerFactory(emf)
                .pageSize(1000)
                .saveState(false);

        if (featureBaseDate == null || featureBaseDate.isBlank()) {
            return builder.queryString("""
                    SELECT fm FROM Monetary fm
                    WHERE fm.featureBaseDate = (SELECT MAX(latest.featureBaseDate) FROM Monetary latest)
                    ORDER BY fm.memberId
                    """).build();
        }

        return builder
                .queryString("SELECT fm FROM Monetary fm WHERE fm.featureBaseDate = :featureBaseDate ORDER BY fm.memberId")
                .parameterValues(Map.of("featureBaseDate", LocalDate.parse(featureBaseDate)))
                .build();
    }
}
