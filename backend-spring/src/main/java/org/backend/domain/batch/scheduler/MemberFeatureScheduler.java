package org.backend.domain.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.backend.domain.batch.service.AnalyticsPipelineService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberFeatureScheduler {

    private final AnalyticsPipelineService analyticsPipelineService;

    @Value("${analytics.full.versioned.enabled:true}")
    private boolean fullVersionedEnabled;

    @Value("${analytics.incremental.enabled:false}")
    private boolean incrementalEnabled;

    @Scheduled(cron = "${analytics.full.cron}", zone = "${analytics.time-zone:Asia/Seoul}")
    @SchedulerLock(
            name = "analytics-feature-pipeline",
            lockAtMostFor = "PT6H",
            lockAtLeastFor = "PT1M"
    )
    public void runDailyFull() {
        if (!fullVersionedEnabled) {
            log.warn("Versioned analytics full pipeline is disabled; running legacy synchronous pipeline");
            analyticsPipelineService.runLegacyDailyFull();
            return;
        }
        analyticsPipelineService.runDailyFull();
    }

    @Scheduled(cron = "${analytics.incremental.cron}", zone = "${analytics.time-zone:Asia/Seoul}")
    @SchedulerLock(
            name = "analytics-feature-pipeline",
            lockAtMostFor = "PT50M",
            lockAtLeastFor = "PT30S"
    )
    public void runHourlyIncremental() {
        if (!incrementalEnabled) {
            return;
        }
        analyticsPipelineService.runHourlyIncremental();
    }

    /**
     * Backward-compatible entry point used by the existing administrator test API.
     */
    @SchedulerLock(
            name = "analytics-feature-pipeline",
            lockAtMostFor = "PT6H",
            lockAtLeastFor = "PT1M"
    )
    public void runMemberFeatureJob() {
        runDailyFull();
    }

    @SchedulerLock(
            name = "analytics-feature-pipeline",
            lockAtMostFor = "PT6H",
            lockAtLeastFor = "PT30S"
    )
    public Boolean retryFailedRun(String batchId) {
        return analyticsPipelineService.retryFailedRun(batchId);
    }
}
