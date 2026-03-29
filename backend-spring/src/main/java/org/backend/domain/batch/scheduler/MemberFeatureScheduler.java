package org.backend.domain.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.domain.analysis.service.DashboardAggregationService;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberFeatureScheduler {

    private final JobLauncher jobLauncher;
    private final Job memberFeatureJob;
    private final Job preAnalysisJob;
    private final Job postAnalysisJob;

    private final RestTemplate restTemplate;
    private final DashboardAggregationService dashboardAggregationService;

    private static final String ANALYTICS_URL = "http://python_server:8000/api/analysis/make";

    // 매일 새벽 2시에 실행
    @Scheduled(cron = "0 0 2 * * *")
    // @Scheduled(cron = "0 * * * * *")
    public void runMemberFeatureJob() {
        try {
            String targetDateStr = LocalDate.now().minusDays(1).toString();
            String targetMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

            log.info("########## [SCHEDULE] BATCH START FOR DATE: {} ##########", targetDateStr);

            // Step 1. Feature Batch
            JobParameters featureParams = new JobParametersBuilder()
                    .addString("featureBaseDate", targetDateStr)
                    .addLong("run.id", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution1 = jobLauncher.run(memberFeatureJob, featureParams);
            if (execution1.getStatus() != BatchStatus.COMPLETED) {
                log.error("########## [SCHEDULE] STEP 1 FAILED! STOPPING PIPELINE ##########");
                return;
            }

            log.info("########## [SCHEDULE] STEP 1 COMPLETED ##########");

            // Step 2. RFM Pre-Analysis
            log.info(">>> Step 2: RFM Pre-Analysis Job Start");

            JobExecution execution2 = jobLauncher.run(
                    preAnalysisJob,
                    new JobParametersBuilder()
                            .addString("baseMonth", targetMonth)
                            .addLong("run.id", System.currentTimeMillis())
                            .toJobParameters()
            );

            if (execution2.getStatus() != BatchStatus.COMPLETED) {
                log.error("########## [SCHEDULE] STEP 2 FAILED! STOPPING PIPELINE ##########");
                return;
            }

            log.info("########## [SCHEDULE] STEP 2 COMPLETED ##########");

            // Step 3. Python Analysis
            log.info(">>> Step 3: Python Analysis Pipeline Start");
            restTemplate.getForEntity(ANALYTICS_URL, String.class);
            log.info("########## [SCHEDULE] STEP 3 COMPLETED ##########");

            // Step 4. KPI / Snapshot Post-Analysis
            log.info(">>> Step 4: KPI Post-Analysis Job Start");

            JobExecution execution4 = jobLauncher.run(
                    postAnalysisJob,
                    new JobParametersBuilder()
                            .addString("baseMonth", targetMonth)
                            .addLong("run.id", System.currentTimeMillis())
                            .toJobParameters()
            );

            if (execution4.getStatus() != BatchStatus.COMPLETED) {
                log.error("########## [SCHEDULE] STEP 4 FAILED! STOPPING PIPELINE ##########");
                return;
            }

            log.info("########## [SCHEDULE] STEP 4 COMPLETED ##########");

            // Step 5. Dashboard Aggregation
            log.info(">>> Step 5: Dashboard Aggregation Start");
            dashboardAggregationService.refreshDashboardStats();
            log.info("########## [SCHEDULE] STEP 5 COMPLETED ##########");

            log.info("########## ALL BATCH PROCESS COMPLETED ##########");

        } catch (Exception e) {
            log.error("########## [SCHEDULE] BATCH FAILED! ##########", e);
        }
    }
}