package org.backend.domain.batch.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.backend.domain.analysis.service.DashboardAggregationService;
import org.backend.domain.batch.client.PythonInferenceClient;
import org.backend.domain.batch.client.PythonInferenceRequest;
import org.backend.domain.batch.client.PythonInferenceResponse;
import org.backend.domain.batch.pipeline.AnalyticsRunContext;
import org.backend.domain.batch.pipeline.AnalyticsRunStatus;
import org.backend.domain.batch.pipeline.AnalyticsRunType;
import org.backend.domain.batch.repository.AnalyticsBatchRunRepository;
import org.backend.domain.batch.repository.ChangedMemberRepository;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
public class AnalyticsPipelineService {

    private final JobLauncher jobLauncher;
    private final Job memberFeatureJob;
    private final Job preAnalysisJob;
    private final Job postAnalysisJob;
    private final AnalyticsBatchRunRepository batchRunRepository;
    private final ChangedMemberRepository changedMemberRepository;
    private final SnapshotPublishService snapshotPublishService;
    private final PythonInferenceClient pythonInferenceClient;
    private final DashboardAggregationService dashboardAggregationService;
    private final MeterRegistry meterRegistry;
    private final AnalyticsPipelineMetrics pipelineMetrics;
    private final Clock clock;
    private final ZoneId businessZone;
    private final Duration incrementalOverlap;
    private final boolean incrementalChurnEnabled;

    public AnalyticsPipelineService(
            @Qualifier("syncJobLauncher") JobLauncher jobLauncher,
            @Qualifier("memberFeatureJob") Job memberFeatureJob,
            @Qualifier("preAnalysisJob") Job preAnalysisJob,
            @Qualifier("postAnalysisJob") Job postAnalysisJob,
            AnalyticsBatchRunRepository batchRunRepository,
            ChangedMemberRepository changedMemberRepository,
            SnapshotPublishService snapshotPublishService,
            PythonInferenceClient pythonInferenceClient,
            DashboardAggregationService dashboardAggregationService,
            MeterRegistry meterRegistry,
            AnalyticsPipelineMetrics pipelineMetrics,
            Clock clock,
            @Value("${analytics.time-zone:Asia/Seoul}") String businessTimeZone,
            @Value("${analytics.incremental.overlap:10m}") Duration incrementalOverlap,
            @Value("${analytics.churn.incremental.enabled:false}") boolean incrementalChurnEnabled) {
        this.jobLauncher = jobLauncher;
        this.memberFeatureJob = memberFeatureJob;
        this.preAnalysisJob = preAnalysisJob;
        this.postAnalysisJob = postAnalysisJob;
        this.batchRunRepository = batchRunRepository;
        this.changedMemberRepository = changedMemberRepository;
        this.snapshotPublishService = snapshotPublishService;
        this.pythonInferenceClient = pythonInferenceClient;
        this.dashboardAggregationService = dashboardAggregationService;
        this.meterRegistry = meterRegistry;
        this.pipelineMetrics = pipelineMetrics;
        this.clock = clock;
        this.businessZone = ZoneId.of(businessTimeZone);
        this.incrementalOverlap = incrementalOverlap;
        this.incrementalChurnEnabled = incrementalChurnEnabled;
    }

    public void runDailyFull() {
        execute(buildDailyFullContext(clock, businessZone));
    }

    public void runLegacyDailyFull() {
        AnalyticsRunContext context = buildDailyFullContext(clock, businessZone);
        String baseMonth = baseMonthFor(context.featureBaseDate());
        String legacyBatchId = "legacy-" + context.featureBaseDate();
        try {
            JobExecution feature = jobLauncher.run(memberFeatureJob, new JobParametersBuilder()
                    .addString("batchId", legacyBatchId)
                    .addString("staged", "false")
                    .addString("featureBaseDate", context.featureBaseDate().toString())
                    .addString("featureBaseAt", context.featureBaseAt().toString())
                    .toJobParameters());
            requireCompleted(feature, "legacy member feature");

            JobExecution pre = jobLauncher.run(preAnalysisJob, new JobParametersBuilder()
                    .addString("baseMonth", baseMonth)
                    .addString("featureBaseDate", context.featureBaseDate().toString())
                    .toJobParameters());
            requireCompleted(pre, "legacy pre analysis");

            pythonInferenceClient.runLegacyFullAnalysis();
            runPostAnalysis(context, legacyBatchId);
            dashboardAggregationService.refreshDashboardStats();
        } catch (JobInstanceAlreadyCompleteException duplicate) {
            log.info("Legacy daily pipeline already completed - featureBaseDate={}",
                    context.featureBaseDate());
        } catch (Exception failure) {
            log.error("Legacy daily pipeline failed - featureBaseDate={}",
                    context.featureBaseDate(), failure);
        }
    }

    public void runHourlyIncremental() {
        LocalDateTime endExclusive = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime lastSuccess = batchRunRepository.lastSuccessfulEndAt()
                .orElse(endExclusive.minusHours(1));
        LocalDateTime startInclusive = lastSuccess.minus(incrementalOverlap);
        LocalDate featureBaseDate = LocalDate.ofInstant(clock.instant(), businessZone);

        AnalyticsRunContext context = new AnalyticsRunContext(
                UUID.randomUUID().toString(),
                AnalyticsRunType.INCREMENTAL,
                featureBaseDate,
                endExclusive,
                startInclusive,
                endExclusive
        );
        execute(context);
    }

    public boolean retryFailedRun(String batchId) {
        AnalyticsRunContext context = batchRunRepository.findContext(batchId).orElse(null);
        if (context == null || !batchRunRepository.resetFailedForRetry(batchId)) {
            return false;
        }
        executeClaimed(context);
        return true;
    }

    static AnalyticsRunContext buildDailyFullContext(Clock clock, ZoneId businessZone) {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(businessZone);
        LocalDate featureBaseDate = now.toLocalDate().minusDays(1);
        Instant windowStart = featureBaseDate.atStartOfDay(businessZone).toInstant();
        Instant windowEnd = featureBaseDate.plusDays(1).atStartOfDay(businessZone).toInstant();

        return new AnalyticsRunContext(
                UUID.randomUUID().toString(),
                AnalyticsRunType.FULL,
                featureBaseDate,
                LocalDateTime.ofInstant(windowEnd, ZoneOffset.UTC),
                LocalDateTime.ofInstant(windowStart, ZoneOffset.UTC),
                LocalDateTime.ofInstant(windowEnd, ZoneOffset.UTC)
        );
    }

    private void execute(AnalyticsRunContext context) {
        if (!batchRunRepository.create(context)) {
            meterRegistry.counter("analytics.overlap.blocked", "runType", context.runType().name()).increment();
            log.info("Analytics window already exists; execution skipped - runType={}, start={}, end={}",
                    context.runType(), context.windowStartAt(), context.windowEndAt());
            return;
        }

        executeClaimed(context);
    }

    private void executeClaimed(AnalyticsRunContext context) {
        Timer.Sample totalTimer = Timer.start(meterRegistry);
        try {
            int targets = context.runType() == AnalyticsRunType.FULL
                    ? batchRunRepository.addAllMembers(context.batchId())
                    : registerIncrementalTargets(context);

            meterRegistry.counter("analytics.batch.targets", "runType", context.runType().name())
                    .increment(targets);

            if (context.runType() == AnalyticsRunType.INCREMENTAL && !incrementalChurnEnabled) {
                batchRunRepository.disableInference(context.batchId());
            }

            if (targets > 0 && !hasCompleteFeatureStaging(context.batchId(), targets)) {
                runFeatureJob(context);
            }
            int featureCount = snapshotPublishService.validateFeatureStaging(context.batchId(), targets);
            snapshotPublishService.prepareRfmStaging(context.batchId());
            batchRunRepository.markFeatureReady(context.batchId(), featureCount);

            int inferenceTargets = batchRunRepository.inferenceTargetCount(context.batchId());
            PythonInferenceResponse inference = null;
            if (inferenceTargets > 0) {
                batchRunRepository.updateStatus(context.batchId(), AnalyticsRunStatus.INFERENCING);
                inference = pythonInferenceClient.infer(
                        new PythonInferenceRequest(
                                context.batchId(),
                                context.featureBaseAt().toString(),
                                context.runType() == AnalyticsRunType.FULL,
                                context.runType() == AnalyticsRunType.FULL
                        ),
                        inferenceTargets
                );
            }

            snapshotPublishService.publish(
                    context,
                    inference,
                    context.runType() == AnalyticsRunType.INCREMENTAL
            );
            pipelineMetrics.recordPublish(context.featureBaseAt(), targets);
            if (inference != null) {
                try {
                    pythonInferenceClient.warmChurnSummaryCache();
                } catch (RuntimeException cacheWarmFailure) {
                    meterRegistry.counter("cache.warm.failure", "cache", "churnSummaryCache").increment();
                    log.warn("Churn summary cache warm failed; persisted current table remains available - batchId={}",
                            context.batchId(), cacheWarmFailure);
                }
            }

            if (context.runType() == AnalyticsRunType.FULL) {
                try {
                    runPostAnalysis(context, context.batchId());
                    dashboardAggregationService.refreshDashboardStats();
                } catch (Exception refreshFailure) {
                    batchRunRepository.markReadyWithWarning(context.batchId(), refreshFailure);
                    meterRegistry.counter("analytics.dashboard.refresh.failure").increment();
                    log.error("Snapshot is ready but post-analysis/dashboard refresh failed - batchId={}",
                            context.batchId(), refreshFailure);
                }
            }

            totalTimer.stop(Timer.builder("analytics.batch.duration")
                    .tag("runType", context.runType().name())
                    .tag("stage", "total")
                    .tag("result", "success")
                    .register(meterRegistry));
            meterRegistry.counter("analytics.batch.success", "runType", context.runType().name())
                    .increment();
            log.info("Analytics pipeline completed - batchId={}, runType={}, targets={}",
                    context.batchId(), context.runType(), targets);
        } catch (Exception failure) {
            batchRunRepository.markFailed(context.batchId(), failure);
            meterRegistry.counter("analytics.batch.failure", "runType", context.runType().name()).increment();
            totalTimer.stop(Timer.builder("analytics.batch.duration")
                    .tag("runType", context.runType().name())
                    .tag("stage", "total")
                    .tag("result", "failure")
                    .register(meterRegistry));
            log.error("Analytics pipeline failed - batchId={}, runType={}",
                    context.batchId(), context.runType(), failure);
        }
    }

    private boolean hasCompleteFeatureStaging(String batchId, int targetCount) {
        try {
            snapshotPublishService.validateFeatureStaging(batchId, targetCount);
            return true;
        } catch (IllegalStateException incomplete) {
            return false;
        }
    }

    private int registerIncrementalTargets(AnalyticsRunContext context) {
        changedMemberRepository.findAndStoreChangedMembers(
                context.batchId(),
                context.windowStartAt(),
                context.windowEndAt()
        );
        return batchRunRepository.refreshTargetCount(context.batchId());
    }

    private void runFeatureJob(AnalyticsRunContext context) throws Exception {
        JobExecution execution = jobLauncher.run(memberFeatureJob, new JobParametersBuilder()
                .addString("batchId", context.batchId())
                .addString("staged", "true")
                .addString("runType", context.runType().name())
                .addString("featureBaseDate", context.featureBaseDate().toString())
                .addString("featureBaseAt", context.featureBaseAt().toString())
                .toJobParameters());
        requireCompleted(execution, "member feature");
    }

    private void runPostAnalysis(AnalyticsRunContext context, String featureBatchId) throws Exception {
        String baseMonth = baseMonthFor(context.featureBaseDate());
        try {
            JobExecution execution = jobLauncher.run(postAnalysisJob, new JobParametersBuilder()
                    .addString("batchId", context.batchId())
                    .addString("featureBatchId", featureBatchId)
                    .addString("baseMonth", baseMonth)
                    .addString("featureBaseDate", context.featureBaseDate().toString())
                    .toJobParameters());
            requireCompleted(execution, "post analysis");
        } catch (JobInstanceAlreadyCompleteException alreadyCompleted) {
            log.info("Post-analysis job was already completed during a prior attempt - batchId={}",
                    context.batchId());
        }
    }

    private void requireCompleted(JobExecution execution, String stage) {
        if (execution.getStatus() != BatchStatus.COMPLETED) {
            Throwable cause = execution.getAllFailureExceptions().stream().findFirst().orElse(null);
            throw new IllegalStateException(stage + " job did not complete", cause);
        }
    }

    static String baseMonthFor(LocalDate featureBaseDate) {
        return featureBaseDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
}
