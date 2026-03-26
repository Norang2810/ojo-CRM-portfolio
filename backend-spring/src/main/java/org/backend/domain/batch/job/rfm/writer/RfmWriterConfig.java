package org.backend.domain.batch.job.rfm.writer;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.backend.domain.analysis.entity.Rfm;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RfmWriterConfig {

    private final EntityManagerFactory emf;

    @Bean
    public JpaItemWriter<Rfm> rfmWriter(){
        return new JpaItemWriterBuilder<Rfm>()
                .entityManagerFactory(emf)
                .build();
    }
}
