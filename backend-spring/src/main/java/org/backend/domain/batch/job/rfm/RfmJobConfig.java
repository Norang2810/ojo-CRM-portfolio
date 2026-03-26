package org.backend.domain.batch.job.rfm;

import lombok.RequiredArgsConstructor;
import org.backend.domain.batch.dto.SnapshotWrapper;
import org.backend.domain.batch.entity.Monetary;
import org.backend.domain.batch.entity.SnapshotBilling;
import org.backend.domain.batch.job.rfm.reader.RfmReaderConfig;
import org.backend.domain.batch.job.rfm.tasklet.KpiTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class RfmJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final RfmReaderConfig rfmReaderConfig;
    private final KpiTasklet kpiTasklet;

    @Bean
    public TaskExecutor taskExecutor1(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("batch-thread-");
        executor.initialize();
        return executor;
    }

    @Bean
    public Job preAnalysisJob(Step rfmStep){
        return new JobBuilder("preAnalysisJob", jobRepository)
                .start(rfmStep)
                .build();
    }

    @Bean
    public Job postAnalysisJob(Step kpiStep, Step snapshotStep){
        return new JobBuilder("postAnalysisJob", jobRepository)
                .start(kpiStep)
                .next(snapshotStep)
                .build();
    }

    @Bean
    public Step rfmStep(ItemProcessor<Monetary, ?> rfmProcessor,
                        ItemWriter<Object> rfmWriter){
        return new StepBuilder("rfmStep", jobRepository)
                .<Monetary, Object>chunk(1000, transactionManager)
                .reader(rfmReaderConfig.rfmReader())
                .processor( (ItemProcessor<? super Monetary, ?>) rfmProcessor )
                .writer(rfmWriter)
                .taskExecutor(taskExecutor1())
                .build();
    }

    @Bean
    public Step kpiStep(){
        return new StepBuilder("kpiStep", jobRepository)
                .tasklet(kpiTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step snapshotStep(ItemReader<SnapshotWrapper> snapshotReader,
                             ItemProcessor<SnapshotWrapper, SnapshotBilling> snapshotProcessor,
                             ItemWriter<Object> snapshotWriter){
        return new StepBuilder("snapshotStep", jobRepository)
                .<SnapshotWrapper, Object>chunk(1000, transactionManager)
                .reader(snapshotReader)
                .processor(snapshotProcessor)
                .writer(snapshotWriter)
                .taskExecutor(taskExecutor1())
                .build();
    }
}
