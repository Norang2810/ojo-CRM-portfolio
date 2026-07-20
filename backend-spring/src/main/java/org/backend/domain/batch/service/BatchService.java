package org.backend.domain.batch.service;

import lombok.RequiredArgsConstructor;
import org.backend.domain.batch.dto.response.BatchHistoryItem;
import org.backend.domain.batch.dto.response.BatchHistoryListResponse;
import org.backend.domain.batch.dto.response.BatchStatusDetailResponse;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BatchService {

    private static final String JOB_NAME = "memberFeatureJob";

    private final JobExplorer jobExplorer;
    private final JobOperator jobOperator;
    private final JdbcTemplate jdbcTemplate;

    public BatchStatusDetailResponse getBatchDetailStatusByCustomId(String customBatchId) {
        Optional<BatchStatusDetailResponse> managed = findManagedRun(customBatchId);
        if (managed.isPresent()) return managed.get();

        JobExecution targetExecution = findExecutionByCustomId(customBatchId);
        return targetExecution == null ? null : convertToDetailResponse(targetExecution);
    }

    public boolean stopJobByCustomId(String customBatchId) throws Exception {
        JobExecution targetExecution = findExecutionByCustomId(customBatchId);
        if (targetExecution != null && targetExecution.isRunning()) {
            return jobOperator.stop(targetExecution.getId());
        }
        return false;
    }

    public BatchHistoryListResponse getBatchHistory() {
        LocalDateTime lastSuccess = lastSuccessfulCompletion();
        List<BatchHistoryItem> managedRuns = jdbcTemplate.query("""
                SELECT batch_id, run_type, feature_base_date, status,
                       window_start_at, window_end_at, target_count,
                       feature_success_count, failure_count, model_version,
                       started_at, completed_at
                FROM analytics_batch_run
                ORDER BY started_at DESC
                LIMIT 30
                """, (resultSet, rowNumber) -> BatchHistoryItem.builder()
                .batchId(resultSet.getString("batch_id"))
                .featureBaseDate(resultSet.getDate("feature_base_date").toLocalDate().toString())
                .batchStatus(resultSet.getString("status"))
                .runType(resultSet.getString("run_type"))
                .windowStartAt(toLocalDateTime(resultSet.getTimestamp("window_start_at")))
                .windowEndAt(toLocalDateTime(resultSet.getTimestamp("window_end_at")))
                .targetCount(resultSet.getLong("target_count"))
                .successCount(resultSet.getLong("feature_success_count"))
                .failureCount(resultSet.getLong("failure_count"))
                .modelVersion(resultSet.getString("model_version"))
                .startTime(toLocalDateTime(resultSet.getTimestamp("started_at")))
                .endTime(toLocalDateTime(resultSet.getTimestamp("completed_at")))
                .lastSuccessfulCompletion(lastSuccess)
                .build());

        if (!managedRuns.isEmpty()) {
            return BatchHistoryListResponse.builder()
                    .totalCount(managedRuns.size())
                    .batchList(managedRuns)
                    .build();
        }

        List<BatchHistoryItem> legacyRuns = jobExplorer.getJobInstances(JOB_NAME, 0, 30).stream()
                .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                .map(execution -> BatchHistoryItem.builder()
                        .batchId(execution.getJobParameters()
                                .getString("batchId", String.valueOf(execution.getId())))
                        .featureBaseDate(execution.getJobParameters().getString("featureBaseDate"))
                        .batchStatus(execution.getStatus().name())
                        .startTime(execution.getStartTime())
                        .endTime(execution.getEndTime())
                        .build())
                .toList();

        return BatchHistoryListResponse.builder()
                .totalCount(legacyRuns.size())
                .batchList(legacyRuns)
                .build();
    }

    private JobExecution findExecutionByCustomId(String customBatchId) {
        return jobExplorer.getJobInstances(JOB_NAME, 0, 100).stream()
                .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                .filter(execution -> customBatchId.equals(
                        execution.getJobParameters().getString("batchId")))
                .findFirst()
                .orElse(null);
    }

    private BatchStatusDetailResponse convertToDetailResponse(JobExecution jobExecution) {
        long readCount = 0;
        long writeCount = 0;
        long skipCount = 0;
        for (StepExecution step : jobExecution.getStepExecutions()) {
            readCount += step.getReadCount();
            writeCount += step.getWriteCount();
            skipCount += step.getProcessSkipCount() + step.getWriteSkipCount();
        }

        return BatchStatusDetailResponse.builder()
                .batchId(jobExecution.getJobParameters()
                        .getString("batchId", String.valueOf(jobExecution.getId())))
                .batchStatus(jobExecution.getStatus().name())
                .featureBaseDate(jobExecution.getJobParameters().getString("featureBaseDate"))
                .totalTargetCount(jobExecution.getJobParameters().getLong("totalTargetCount", 0L))
                .processedCount(readCount)
                .successCount(writeCount)
                .failCount(skipCount)
                .startTime(jobExecution.getStartTime())
                .endTime(jobExecution.getEndTime())
                .build();
    }

    private Optional<BatchStatusDetailResponse> findManagedRun(String batchId) {
        List<BatchStatusDetailResponse> runs = jdbcTemplate.query("""
                        SELECT batch_id, run_type, feature_base_date, status,
                               window_start_at, window_end_at, target_count,
                               feature_success_count, failure_count, model_version,
                               started_at, completed_at
                        FROM analytics_batch_run WHERE batch_id = ?
                        """,
                (resultSet, rowNumber) -> {
                    long success = resultSet.getLong("feature_success_count");
                    long failure = resultSet.getLong("failure_count");
                    return BatchStatusDetailResponse.builder()
                            .batchId(resultSet.getString("batch_id"))
                            .batchStatus(resultSet.getString("status"))
                            .featureBaseDate(resultSet.getDate("feature_base_date").toLocalDate().toString())
                            .totalTargetCount(resultSet.getLong("target_count"))
                            .processedCount(success + failure)
                            .successCount(success)
                            .failCount(failure)
                            .startTime(toLocalDateTime(resultSet.getTimestamp("started_at")))
                            .endTime(toLocalDateTime(resultSet.getTimestamp("completed_at")))
                            .runType(resultSet.getString("run_type"))
                            .windowStartAt(toLocalDateTime(resultSet.getTimestamp("window_start_at")))
                            .windowEndAt(toLocalDateTime(resultSet.getTimestamp("window_end_at")))
                            .modelVersion(resultSet.getString("model_version"))
                            .lastSuccessfulCompletion(lastSuccessfulCompletion())
                            .build();
                },
                batchId
        );
        return runs.stream().findFirst();
    }

    private LocalDateTime lastSuccessfulCompletion() {
        return jdbcTemplate.query("""
                        SELECT MAX(completed_at) AS completed_at
                        FROM analytics_batch_run
                        WHERE status IN ('READY', 'READY_WITH_ERRORS')
                        """,
                resultSet -> resultSet.next()
                        ? toLocalDateTime(resultSet.getTimestamp("completed_at"))
                        : null
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
