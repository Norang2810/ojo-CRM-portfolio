package org.backend.domain.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
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
    private final String ANALYTICS_URL = "http://python_server:8000/api/analysis/make";

    // 매일 새벽 2시에 실행 (Cron 표현식: 초 분 시 일 월 요일)
    @Scheduled(cron = "0 0 2 * * *")
//    @Scheduled(cron = "0 * * * * *")
    public void runMemberFeatureJob() {
        try {
            // 어제 날짜를 기준일로 설정 (데이터 정합성 측면에서 유리)
            String targetDateStr = LocalDate.now().minusDays(1).toString();
            String targetMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            log.info("########## [SCHEDULE] BATCH START FOR DATE: {} ##########", targetDateStr);

            JobParameters params = new JobParametersBuilder()
                    .addString("featureBaseDate", targetDateStr)
                    .addLong("run.id", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution1 = jobLauncher.run(memberFeatureJob, params);
            if (execution1.getStatus() != BatchStatus.COMPLETED) {
                log.error("########## [SCHEDULE] STEP 1 FAILED! STOPPING PIPELINE ##########");
                return;
            }

            log.info("########## [SCHEDULE] BATCH COMPLETED SUCCESSFULLY ##########");

            log.info(">>> Step 2: RFM Pre-Analysis Job Start");

            JobExecution execution2 = jobLauncher.run(preAnalysisJob, new JobParametersBuilder()
                    .addString("baseMonth", targetMonth)
                    .addLong("run.id", System.currentTimeMillis()).toJobParameters());
            if (execution2.getStatus() != BatchStatus.COMPLETED) {
                log.error("########## [SCHEDULE] STEP 2 FAILED! STOPPING PIPELINE ##########");
                return;
            }

            // 3. 파이썬 다차원 분석 호출 (LTV, Churn, Recommend)
            log.info(">>> Step 3: Python Analysis Pipeline Start");
            restTemplate.getForEntity(ANALYTICS_URL, String.class);

            // 4. KPI 및 스냅샷 생성
            log.info(">>> Step 4: KPI Post-Analysis Job Start");
            JobExecution execution4 = jobLauncher.run(postAnalysisJob, new JobParametersBuilder()
                    .addString("baseMonth", targetMonth)
                    .addLong("run.id", System.currentTimeMillis()).toJobParameters());
            if (execution4.getStatus() != BatchStatus.COMPLETED) {
                log.error("########## [SCHEDULE] STEP 4 FAILED! STOPPING PIPELINE ##########");
                return;
            }

            log.info("########## ALL BATCH PROCESS COMPLETED ##########");
        } catch (Exception e) {
            log.error("########## [SCHEDULE] BATCH FAILED! ##########", e);
        }
    }
}