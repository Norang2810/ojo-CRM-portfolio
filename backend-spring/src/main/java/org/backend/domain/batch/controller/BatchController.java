package org.backend.domain.batch.controller;

import lombok.RequiredArgsConstructor;
import org.backend.common.CommonResponse;
import org.backend.domain.batch.dto.response.BatchHistoryListResponse;
import org.backend.domain.batch.dto.response.BatchStatusDetailResponse;
import org.backend.domain.batch.dto.response.MemberFeatureResponse;
import org.backend.domain.batch.scheduler.MemberFeatureScheduler;
import org.backend.domain.batch.service.BatchResetService;
import org.backend.domain.batch.service.BatchService;
import org.backend.domain.batch.service.MemberFeatureService;
import org.backend.domain.member.repository.MemberRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/batch")
public class BatchController {

    @Qualifier("asyncJobLauncher")
    private final JobLauncher jobLauncher;

    private final Job memberFeatureJob;
    private final JobExplorer jobExplorer;
    private final MemberRepository memberRepository;
    private final BatchService batchService;

    private final Job preAnalysisJob;
    private final Job postAnalysisJob;
    private final BatchResetService batchResetService;
    private final MemberFeatureService memberFeatureService;

    private final MemberFeatureScheduler memberFeatureScheduler;

    // 1. 배치 실행
    @PostMapping("/run")
    public CommonResponse<String> runBatch() throws Exception {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long instanceCount = jobExplorer.getJobInstanceCount("memberFeatureJob");
        String batchId = today + "_" + String.format("%04d", instanceCount + 1);

        long memberCount = memberRepository.count();
        int stepCount = 4;
        long totalTargetCount = memberCount * stepCount;

        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addString("batchId", batchId)
                .addString("featureBaseDate", LocalDate.now().toString())
                .addLong("totalTargetCount", totalTargetCount)
                .toJobParameters();

        jobLauncher.run(memberFeatureJob, params);

        return CommonResponse.success(batchId, "Batch Started");
    }

    // 2. 배치 실행 이력 조회
    @GetMapping("/history")
    public CommonResponse<BatchHistoryListResponse> getHistory() {
        BatchHistoryListResponse data = batchService.getBatchHistory();

        return CommonResponse.success(data, "배치 이력 조회 성공");
    }

    // 3. 배치 상태 상세 조회
    @GetMapping("/status/{batchId}")
    public CommonResponse<BatchStatusDetailResponse> getStatus(@PathVariable String batchId) {
        BatchStatusDetailResponse data = batchService.getBatchDetailStatusByCustomId(batchId);
        if (data == null) {
            return CommonResponse.fail("해당 batchId의 실행 이력을 찾을 수 없습니다."); //
        }
        return CommonResponse.success(data, "배치 상태 조회 성공");
    }

    // 4. 배치 중지
    @PostMapping("/stop/{batchId}")
    public CommonResponse<String> stopJob(@PathVariable String batchId) {
        try {
            boolean stopped = batchService.stopJobByCustomId(batchId);
            return stopped ? CommonResponse.success("Batch ID: " + batchId, "중지 요청 성공")
                    : CommonResponse.fail("중지 가능한 상태(RUNNING)가 아니거나 존재하지 않습니다.");
        } catch (Exception e) {
            return CommonResponse.fail("중지 중 오류 발생: " + e.getMessage());
        }
    }

    // 5. 고객 특성 조회
    @GetMapping("/feature/{memberId}")
    public CommonResponse<MemberFeatureResponse> getFeatures(@PathVariable Long memberId) {
        MemberFeatureResponse data = memberFeatureService.getMemberFeatures(memberId);

        if (data.getLifecycle() == null && data.getMonetary() == null) {
            return CommonResponse.fail("해당 고객의 배치 분석 데이터가 존재하지 않습니다.");
        }

        return CommonResponse.success(data, "고객 특성 조회 성공");
    }

    @PostMapping("/run/test")
    public String runBatchTest() throws Exception {

        JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(memberFeatureJob, params);

        return "Batch Started";
    }

    @PostMapping("/run/pre")
    public String runPreAnalysis(@RequestParam String baseMonth) throws Exception{
        JobParameters params = new JobParametersBuilder()
                .addString("baseMonth", baseMonth)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(preAnalysisJob, params);

        if (execution.getStatus().isUnsuccessful()) {
            // 실패 원인 중 첫 번째를 에러 메시지로
            Throwable firstException = execution.getAllFailureExceptions().get(0);
            throw new RuntimeException("배치 실패 원인: " + firstException.getMessage());
        }

        return "rfm batch started for " + baseMonth;
    }

    @PostMapping("/run/post")
    public String runPostAnalysis(@RequestParam String baseMonth) throws Exception{
        JobParameters params = new JobParametersBuilder()
                .addString("baseMonth", baseMonth)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(postAnalysisJob, params);

        if(execution.getStatus().isUnsuccessful()){
            Throwable firstException = execution.getAllFailureExceptions().get(0);
            throw new RuntimeException("배치 실패 원인: "+ firstException.getMessage());
        }

        return "kpi and snapshot batch started for " + baseMonth;
    }

    @DeleteMapping("/del")
    public ResponseEntity<CommonResponse<String>> deleteOldData(){
        int count = batchResetService.deleteOldData();
        return ResponseEntity.ok(CommonResponse.success(null, "5년 이상 된 데이터 " + count + "건 삭제 완료"));
    }

    // 분석 데이터 초기화
    @DeleteMapping("/reset")
    public ResponseEntity<CommonResponse<String>> resetBatchData(
            @RequestParam(required = false) List<String> months,
            @RequestParam(defaultValue = "false") boolean isAll
    ){
        batchResetService.resetBatchData(months, isAll);
        String msg = isAll ? "전체 데이터 초기화 완료" : months + "달 데이터 초기화 완료";
        return ResponseEntity.ok(CommonResponse.success(null, msg));
    }

    // 스케줄러 테스트
    @PostMapping("/test/pipeline")
    public String testPipeline() {
        memberFeatureScheduler.runMemberFeatureJob(); // 스케줄러 메서드 직접 호출
        return "Full Pipeline Started Check Logs!";
    }
}