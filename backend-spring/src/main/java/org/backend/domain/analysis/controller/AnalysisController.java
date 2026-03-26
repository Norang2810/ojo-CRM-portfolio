package org.backend.domain.analysis.controller;

import lombok.RequiredArgsConstructor;
import org.backend.common.CommonResponse;
import org.backend.domain.analysis.dto.*;
import org.backend.domain.analysis.service.AnalysisService;
import org.backend.domain.analysis.service.DashboardService;
import org.backend.domain.analysis.service.KpiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {
    private final AnalysisService analysisService;
    private final KpiService kpiService;
    private final DashboardService dashboardService;

    // 특정 고객 LTV 조회
    @GetMapping("/ltv/{memberId}")
    public ResponseEntity<LtvResponseDto> getLtv(@PathVariable Long memberId) {
        return ResponseEntity.ok(analysisService.getLtvDetail(memberId));
    }

    // 고객 통합 분석 요약
    @GetMapping("/summary/{memberId}")
    public ResponseEntity<AnalysisSummaryResponseDto> getAnalysisSummary(@PathVariable Long memberId) {
        return ResponseEntity.ok(analysisService.getAnalysisSummary(memberId));
    }

    // 대시보드 요약 (Cards, DailyStats, Segments)
    @GetMapping("/dashboard")
    public ResponseEntity<CommonResponse<DashboardSummaryResponseDto>> getDashboardSummary() {
        DashboardSummaryResponseDto data = dashboardService.getDashboardSummary();
        return ResponseEntity.ok(CommonResponse.success(data, "대시보드 요약 조회 성공"));
    }

    // 대시보드 - 세그먼트
    @GetMapping("/segements")
    public ResponseEntity<CommonResponse<List<org.backend.domain.analysis.dto.SegmentDataDto>>> getSegments() {
        List<org.backend.domain.analysis.dto.SegmentDataDto> data = analysisService.getSegments();
        return ResponseEntity.ok(CommonResponse.success(data, "고객 분류 조회 성공"));
    }

    // rfm 조회
    @GetMapping("/rfm/{memberId}")
    public ResponseEntity<CommonResponse<RfmResponseDto>> getRfm(@PathVariable Long memberId) {
        RfmResponseDto data = analysisService.getRfm(memberId);
        return ResponseEntity.ok(CommonResponse.success(data, null));
    }

    // 전체 rfm 조회
    @GetMapping("/rfm/segments")
    public ResponseEntity<CommonResponse<RfmSegmentResponseDto>> getRfmSegment(@RequestParam(name = "baseMonth") String baseMonth){
        RfmSegmentResponseDto data = analysisService.getAllRfm(baseMonth);
        String msg = String.format("%s 세그먼트별 RFM 조회 성공", baseMonth);
        return ResponseEntity.ok(CommonResponse.success(data, msg));
    }

    @GetMapping("/rfm/trend")
    public ResponseEntity<CommonResponse<List<RfmTrendResponseDto>>> getRfmTrend(@RequestParam(name = "months", defaultValue = "3") int months) {
        List<RfmTrendResponseDto> data = analysisService.getRfmTrend(months);
        String message = String.format("최근 %d개월간 RFM 트렌드 조회 성공", months);
        return ResponseEntity.ok(CommonResponse.success(data, message));
    }

    // kpi 도출
    @GetMapping("/rfmkpi")
    public ResponseEntity<CommonResponse<RfmKpiResponseDto>> getKpi(
            @RequestParam(name = "baseMonth") String baseMonth) {
        RfmKpiResponseDto data = kpiService.getKpi(baseMonth);
        return ResponseEntity.ok(CommonResponse.success(data, "kpi 조회 성공"));
    }
}
