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

@Configuration
@RequiredArgsConstructor
public class MemberReaderConfig {

    private final EntityManagerFactory emf;

    @Bean
    @StepScope
    public JpaPagingItemReader<Member> consultationMemberReader() {
        return buildReader("consultationMemberReader");
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Member> lifecycleMemberReader() {
        return buildReader("lifecycleMemberReader");
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Member> monetaryMemberReader() {
        return buildReader("monetaryMemberReader");
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Member> usageMemberReader() {
        return buildReader("usageMemberReader");
    }


    private JpaPagingItemReader<Member> buildReader(String name) {
        return new JpaPagingItemReaderBuilder<Member>()
                .name(name)
                .entityManagerFactory(emf)
                .queryString("SELECT m FROM Member m LEFT JOIN FETCH m.consent")
                .pageSize(1000)
                .saveState(false)
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