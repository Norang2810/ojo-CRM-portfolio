package org.backend.domain.batch.job.memberfeature.reader;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.backend.domain.member.entity.Member;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class MemberReaderConfig {

    private final EntityManagerFactory emf;

    @Bean
    @StepScope
    public JpaPagingItemReader<Member> consultationMemberReader(
            @Value("#{jobParameters['batchId']}") String batchId,
            @Value("#{jobParameters['staged']}") String staged,
            @Value("${analytics.feature.reader-page-size:1000}") int pageSize) {
        return buildReader("consultationMemberReader", batchId, staged, pageSize);
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Member> lifecycleMemberReader(
            @Value("#{jobParameters['batchId']}") String batchId,
            @Value("#{jobParameters['staged']}") String staged,
            @Value("${analytics.feature.reader-page-size:1000}") int pageSize) {
        return buildReader("lifecycleMemberReader", batchId, staged, pageSize);
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Member> monetaryMemberReader(
            @Value("#{jobParameters['batchId']}") String batchId,
            @Value("#{jobParameters['staged']}") String staged,
            @Value("${analytics.feature.reader-page-size:1000}") int pageSize) {
        return buildReader("monetaryMemberReader", batchId, staged, pageSize);
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Member> usageMemberReader(
            @Value("#{jobParameters['batchId']}") String batchId,
            @Value("#{jobParameters['staged']}") String staged,
            @Value("${analytics.feature.reader-page-size:1000}") int pageSize) {
        return buildReader("usageMemberReader", batchId, staged, pageSize);
    }


    private JpaPagingItemReader<Member> buildReader(String name, String batchId, String staged, int pageSize) {
        JpaPagingItemReaderBuilder<Member> builder = new JpaPagingItemReaderBuilder<Member>()
                .name(name)
                .entityManagerFactory(emf)
                .pageSize(pageSize)
                .saveState(false);

        if (!Boolean.parseBoolean(staged) || batchId == null || batchId.isBlank()) {
            return builder
                    .queryString("SELECT m FROM Member m LEFT JOIN FETCH m.consent ORDER BY m.id")
                    .build();
        }

        return builder
                .queryString("""
                        SELECT m FROM Member m
                        LEFT JOIN FETCH m.consent
                        WHERE EXISTS (
                            SELECT t.memberId FROM AnalyticsBatchTargetEntity t
                            WHERE t.batchId = :batchId AND t.memberId = m.id
                        )
                        ORDER BY m.id
                        """)
                .parameterValues(Map.of("batchId", batchId))
                .build();
    }


    @Bean
    @Profile("test")
    @StepScope
    public JpaPagingItemReader<Member> memberReaderTest() {
        return new JpaPagingItemReaderBuilder<Member>()
                .name("memberReaderTest")
                .entityManagerFactory(emf)
                .queryString("SELECT m FROM Member m")
                .pageSize(1000)
                .maxItemCount(10)
                .saveState(false)
                .build();
    }



}
