package org.backend.domain.analysis.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.backend.domain.analysis.dto.*;
import org.backend.domain.analysis.entity.Analysis;
import org.backend.domain.analysis.entity.Rfm;
import org.backend.domain.analysis.repository.AnalysisRepository;
import org.backend.domain.analysis.repository.RfmRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalysisService {
        private final AnalysisRepository analysisRepository;
        private final RfmRepository rfmRepository;

        // 특정 고객 LTV 조회 - 고객 1인당 평균 구매단가 * 평균 구매 빈도 * 평균 고객 수명 => 계산은 파이썬에서 진행
        public LtvResponseDto getLtvDetail(Long memberId) {
                return analysisRepository.findFirstByMemberIdOrderByCreatedAtDesc(memberId)
                                .map(a -> new LtvResponseDto(
                                                a.getMember().getId(), a.getLtv(), a.getLifecycleStage()))
                                .orElseThrow(() -> new EntityNotFoundException("분석 데이터를 찾을 수 없습니다."));
        }

        // 고객 통합 분석 요약
        public AnalysisSummaryResponseDto getAnalysisSummary(Long memberId) {
                return analysisRepository.findFirstByMemberIdOrderByCreatedAtDesc(memberId)
                                .map(a -> new AnalysisSummaryResponseDto(
                                                a.getMember().getId(), a.getType(), a.getRfmScore(), a.getLtv(),
                                                a.getLifecycleStage()))
                                .orElseThrow(() -> new EntityNotFoundException("분석 데이터를 찾을 수 없습니다."));
        }

        // 대시보드 - avg_order_val* purchase_cycle*(1/churn_rate)
        // public getTotal(){}

        public List<SegmentDataDto> getSegments() {
                List<Analysis> analyses = analysisRepository.findAllWithMember();

                Map<String, List<Analysis>> grouped = analyses.stream()
                                .filter(a -> a.getType() != null)
                                .collect(Collectors.groupingBy(Analysis::getType));

                return grouped.entrySet().stream()
                                .map(entry -> {
                                        String segmentStr = convertSegmentName(entry.getKey());
                                        List<SegmentMemberDto> members = entry.getValue().stream()
                                                        .map(a -> {
                                                                double computedChurnScore = a.getRfmScore() != null
                                                                                ? Math.max(0.01, 1.0 - (a.getRfmScore()
                                                                                                / 15.0))
                                                                                : 0.85;
                                                                return new SegmentMemberDto(
                                                                                a.getMember().getId(),
                                                                                a.getMember().getName(),
                                                                                a.getRfmScore(),
                                                                                Math.round(computedChurnScore * 100.0)
                                                                                                / 100.0,
                                                                                a.getLifecycleStage());
                                                        })
                                                        .collect(Collectors.toList());

                                        return new SegmentDataDto(segmentStr, members.size(), members);
                                })
                                .collect(Collectors.toList());
        }

        private String convertSegmentName(String dbType) {
                if (dbType == null)
                        return "common";
                return switch (dbType) {
                        case "VIP" -> "vip";
                        case "LOYAL" -> "loyal";
                        case "COMMON" -> "common";
                        case "RISK", "SLEEP" -> "churn_risk";
                        case "LOST" -> "lost";
                        default -> dbType.toLowerCase();
                };
        }

        // 사용자별 rfm 조회
        public RfmResponseDto getRfm(Long memberId) {
                Rfm rfm = rfmRepository.findFirstByMemberIdOrderByUpdatedAtDesc(memberId)
                                .orElseThrow(() -> new EntityNotFoundException("RFM 상세 데이터를 찾을 수 없습니다."));

                Analysis analysis = analysisRepository.findFirstByMemberIdOrderByCreatedAtDesc(memberId)
                                .orElseThrow(() -> new EntityNotFoundException("분석 요약 데이터를 찾을 수 없습니다."));

                RfmResponseDto.RfmDetail detail = new RfmResponseDto.RfmDetail(
                        rfm.getRecency(),
                        rfm.getFrequency(),
                        rfm.getMonetary(),
                        rfm.getUpdatedAt(),
                        analysis.getRfmScore(),
                        analysis.getRScore(),
                        analysis.getFScore(),
                        analysis.getMScore(),
                        analysis.getType());

                return new RfmResponseDto(
                                rfm.getMemberId(), detail);
        }

        // 고객 전체 세그먼트별 rfm 조회
        @Transactional(readOnly = true)
        public RfmSegmentResponseDto getAllRfm(String baseMonth) {
                if (baseMonth == null || baseMonth.isEmpty()) {
                        LocalDateTime latest = analysisRepository.findLatestCreatedAt();
                        if (latest == null)
                                return new RfmSegmentResponseDto(0L, List.of());
                        baseMonth = latest.format(DateTimeFormatter.ofPattern("yyyyMM"));
                } else {
                        baseMonth = baseMonth.replace("-", "");
                }

                List<RfmSegmentResponseDto.SegmentDetail> segmentDetailList = analysisRepository
                                .findRfmStatisticsBySegment(baseMonth);

                Long totalCount = segmentDetailList.stream().mapToLong(RfmSegmentResponseDto.SegmentDetail::count)
                                .sum();

                List<RfmSegmentResponseDto.SegmentDetail> calc = segmentDetailList.stream()
                                .map(s -> new RfmSegmentResponseDto.SegmentDetail(
                                                s.type(),
                                                s.count(),
                                                totalCount == 0 ? 0
                                                                : Math.round((s.count() * 100.0 / totalCount) * 100)
                                                                                / 100.0,
                                                s.avgR(), s.avgF(), s.avgM()))
                                .toList();

                return new RfmSegmentResponseDto(totalCount, calc);
        }

        @Transactional(readOnly = true)
        public List<RfmTrendResponseDto> getRfmTrend(int months) {
                LocalDateTime start = LocalDateTime.now().minusMonths(months);
                return analysisRepository.findRfmTrend(start);
        }
}
