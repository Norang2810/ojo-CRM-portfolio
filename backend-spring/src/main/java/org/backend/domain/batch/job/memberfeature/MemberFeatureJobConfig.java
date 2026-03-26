package org.backend.domain.batch.job.memberfeature;

import lombok.RequiredArgsConstructor;
import org.backend.domain.batch.entity.ConsultationBasics;
import org.backend.domain.batch.entity.FeatureUsage;
import org.backend.domain.batch.entity.Lifecycle;
import org.backend.domain.batch.entity.Monetary;
import org.backend.domain.batch.job.memberfeature.reader.ChunkMemberReader;
import org.backend.domain.member.entity.Member;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
public class MemberFeatureJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;


    @Qualifier("stepTaskExecutor")
    private final ThreadPoolTaskExecutor stepTaskExecutor;

    private static final int CHUNK_SIZE = 1000;

    private <T> void writeFlattened(Chunk<? extends List<T>> items, JdbcBatchItemWriter<T> writer) throws Exception {
        List<T> flat = items.getItems().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        writer.write(new Chunk<>(flat));
    }

    @Bean
    public Job memberFeatureJob(Step consultationStep,
                                Step lifecycleStep,
                                Step monetaryStep,
                                Step usageStep) {
        return new JobBuilder("memberFeatureJob", jobRepository)
                .start(consultationStep)
                .next(lifecycleStep)
                .next(monetaryStep)
                .next(usageStep)
                .build();
    }

    @Bean
    public Step consultationStep(
            ItemProcessor<List<Member>, List<ConsultationBasics>> consultationProcessor,
            JdbcBatchItemWriter<ConsultationBasics> consultationWriter,
            JpaPagingItemReader<Member> consultationMemberReader) {

        return new StepBuilder("consultationStep", jobRepository)
                .<List<Member>, List<ConsultationBasics>>chunk(1, transactionManager)
                .reader(new ChunkMemberReader(consultationMemberReader, CHUNK_SIZE))
                .processor(consultationProcessor)
                .writer(items -> writeFlattened(items, consultationWriter))
                .taskExecutor(stepTaskExecutor)
                .build();
    }

    @Bean
    public Step lifecycleStep(
            ItemProcessor<List<Member>, List<Lifecycle>> memberLifecycleProcessor,
            JdbcBatchItemWriter<Lifecycle> lifecycleWriter,
            JpaPagingItemReader<Member> lifecycleMemberReader) {

        return new StepBuilder("lifecycleStep", jobRepository)
                .<List<Member>, List<Lifecycle>>chunk(1, transactionManager)
                .reader(new ChunkMemberReader(lifecycleMemberReader, CHUNK_SIZE))
                .processor(memberLifecycleProcessor)
                .writer(items -> writeFlattened(items, lifecycleWriter))
                .taskExecutor(stepTaskExecutor)
                .build();
    }

    @Bean
    public Step monetaryStep(
            ItemProcessor<List<Member>, List<Monetary>> monetaryProcessor,
            JdbcBatchItemWriter<Monetary> monetaryWriter,
            JpaPagingItemReader<Member> monetaryMemberReader) {

        return new StepBuilder("monetaryStep", jobRepository)
                .<List<Member>, List<Monetary>>chunk(1, transactionManager)
                .reader(new ChunkMemberReader(monetaryMemberReader, CHUNK_SIZE))
                .processor(monetaryProcessor)
                .writer(items -> writeFlattened(items, monetaryWriter))
                .taskExecutor(stepTaskExecutor)
                .build();
    }

    @Bean
    public Step usageStep(
            ItemProcessor<List<Member>, List<FeatureUsage>> usageProcessor,
            JdbcBatchItemWriter<FeatureUsage> usageWriter,
            JpaPagingItemReader<Member> usageMemberReader) {

        return new StepBuilder("usageStep", jobRepository)
                .<List<Member>, List<FeatureUsage>>chunk(1, transactionManager)
                .reader(new ChunkMemberReader(usageMemberReader, CHUNK_SIZE))
                .processor(usageProcessor)
                .writer(items -> writeFlattened(items, usageWriter))
                .taskExecutor(stepTaskExecutor)
                .build();
    }
}