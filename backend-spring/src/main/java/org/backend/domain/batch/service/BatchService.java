package org.backend.domain.batch.service;

import lombok.RequiredArgsConstructor;
import org.backend.domain.batch.dto.response.BatchHistoryItem;
import org.backend.domain.batch.dto.response.BatchHistoryListResponse;
import org.backend.domain.batch.dto.response.BatchStatusDetailResponse;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BatchService {

    private final JobExplorer jobExplorer;
    private final JobOperator jobOperator; // 배치 중지를 위해 필요
    private static final String JOB_NAME = "memberFeatureJob";


    public BatchStatusDetailResponse getBatchDetailStatusByCustomId(String customBatchId) {
        // 최근 실행된 인스턴스들에서 해당 batchId 파라미터를 가진 실행 건 찾기
        JobExecution targetExecution = findExecutionByCustomId(customBatchId);

        if (targetExecution == null) return null;

        return convertToDetailResponse(targetExecution);
    }

   // 배치 중지
    public boolean stopJobByCustomId(String customBatchId) throws Exception {
        JobExecution targetExecution = findExecutionByCustomId(customBatchId);
        if (targetExecution != null && targetExecution.isRunning()) {
            return jobOperator.stop(targetExecution.getId());
        }
        return false;
    }

    // 전체 실행 이력 조회
    public BatchHistoryListResponse getBatchHistory() {
        List<JobExecution> executions = jobExplorer.getJobInstances(JOB_NAME, 0, 30).stream()
                .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                .collect(Collectors.toList());

        List<BatchHistoryItem> batchList = executions.stream()
                .map(ex -> BatchHistoryItem.builder()
                        .batchId(ex.getJobParameters().getString("batchId", String.valueOf(ex.getId())))
                        .featureBaseDate(ex.getJobParameters().getString("featureBaseDate"))
                        .batchStatus(ex.getStatus().name())
                        .startTime(ex.getStartTime())
                        .endTime(ex.getEndTime())
                        .build())
                .collect(Collectors.toList());

        return BatchHistoryListResponse.builder()
                .totalCount(batchList.size())
                .batchList(batchList)
                .build();
    }


    private JobExecution findExecutionByCustomId(String customBatchId) {
        return jobExplorer.getJobInstances(JOB_NAME, 0, 100).stream()
                .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                .filter(ex -> customBatchId.equals(ex.getJobParameters().getString("batchId")))
                .findFirst()
                .orElse(null);
    }


    private BatchStatusDetailResponse convertToDetailResponse(JobExecution jobExecution) {
        Long totalTargetFromParam = jobExecution.getJobParameters().getLong("totalTargetCount", 0L);

        long readCount = 0;
        long writeCount = 0;
        long skipCount = 0;

        for (StepExecution se : jobExecution.getStepExecutions()) {
            readCount += se.getReadCount();
            writeCount += se.getWriteCount();
            skipCount += (se.getProcessSkipCount() + se.getWriteSkipCount());
        }

        return BatchStatusDetailResponse.builder()
                .batchId(jobExecution.getJobParameters().getString("batchId", String.valueOf(jobExecution.getId())))
                .batchStatus(jobExecution.getStatus().name())
                .featureBaseDate(jobExecution.getJobParameters().getString("featureBaseDate"))
                .totalTargetCount(totalTargetFromParam)
                .processedCount(readCount)
                .successCount(writeCount)
                .failCount(skipCount)
                .startTime(jobExecution.getStartTime())
                .endTime(jobExecution.getEndTime())
                .build();
    }
}