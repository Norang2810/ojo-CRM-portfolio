package org.backend.domain.batch.job.rfm.writer;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.backend.domain.batch.entity.SnapshotBilling;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SnapshotWriterConfig {

    private final EntityManagerFactory emf;

    @Bean
    public JpaItemWriter<SnapshotBilling> snapshotWriter(){
        return new JpaItemWriterBuilder<SnapshotBilling>()
                .entityManagerFactory(emf)
                .build();
    }
}
