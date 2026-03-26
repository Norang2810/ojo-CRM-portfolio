package org.backend.domain.batch.job.rfm.reader;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.backend.domain.batch.entity.Monetary;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RfmReaderConfig {

    private final EntityManagerFactory emf;

    @Bean
    @StepScope
    public JpaPagingItemReader<Monetary> rfmReader() {
        return new JpaPagingItemReaderBuilder<Monetary>()
                .name("rfmReader")
                .entityManagerFactory(emf)
                .queryString("SELECT fm FROM Monetary fm")
                .pageSize(1000)
                .saveState(false)
                .build();
    }
}
