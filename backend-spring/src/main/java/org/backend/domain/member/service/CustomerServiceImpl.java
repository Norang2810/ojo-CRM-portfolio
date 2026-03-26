package org.backend.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.backend.common.CommonResponse;
import org.backend.common.exception.CustomException;
import org.backend.common.exception.ErrorCode;
import org.backend.common.page.PageResponse;
import org.backend.domain.advice.repository.AdviceRepository;
import org.backend.domain.advice.view.CustomerConsultView;
import org.backend.domain.analysis.repository.CustomerSummaryRepository;

import org.backend.domain.analysis.repository.FeatureConsultationRepository;
import org.backend.domain.member.dto.*;
import org.backend.domain.member.entity.Member;
import org.backend.domain.member.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private final MemberRepository memberRepository;
    private final CustomerSummaryRepository summaryRepository;
    private final FeatureConsultationRepository featureRepository;
    private final AdviceRepository adviceRepository;

    // 고객별 기본 정보 조회
    @Override
    public CommonResponse<CustomerDetailResponse> getCustomerDetail(Long memberId) {

        Member member = memberRepository.findWithConsentById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        CustomerDetailResponse response = CustomerDetailResponse.from(member);

        return CommonResponse.success(response, "고객 기본 정보 조회");
    }

    // 고객 전체 리스트
    @Override
    public CommonResponse<PageResponse<CustomerSummaryResponse>> getCustomerList(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        // 통계 가져오기
        ConsultStatProjection stats = featureRepository.getConsultStats();

        double avg = stats != null && stats.getAvgValue() != null ? stats.getAvgValue() : 0;

        double std = stats != null && stats.getStdValue() != null ? stats.getStdValue() : 0;

        // 고객 요약 조회
        Page<CustomerSummaryProjection> result = summaryRepository.findCustomerSummary(pageable);

        // 매핑
        Page<CustomerSummaryResponse> mapped = result.map(p -> new CustomerSummaryResponse(
                p.getMemberId(),
                p.getName(),
                maskEmail(p.getEmail()),
                maskPhone(p.getPhone()),
                p.getProductName(),
                p.getCreatedAt().toLocalDate() + " ~ 현재",
                p.getTopConsultCategory(),
                calculateFrequency(p.getLast30dConsultCount(), avg, std),
                convertVipType(p.getVipType())
        ));

        return CommonResponse.success(
                PageResponse.from(mapped),
                "고객 목록 조회");
    }
    // 정렬 추가 - 작동 테스트 후 위 서비스 삭제
    @Override
    public CommonResponse<PageResponse<CustomerSummaryResponse>> getCustomerList(int page, int size, List<MemberSortRequest> sorts) {
        // 다중 정렬
        Sort sort = Sort.unsorted();
        if(sorts != null && !sorts.isEmpty()){
            List<Sort.Order> orders = sorts.stream()
                .map(s -> new Sort.Order(
                    s.getOrder().equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
                    sortField(s.getField())
                ))
                .toList();
            sort = Sort.by(orders);
        }

        Pageable pageable = PageRequest.of(page, size, sort);

        // 통계 가져오기
        ConsultStatProjection stats = featureRepository.getConsultStats();

        double avg = stats != null && stats.getAvgValue() != null ? stats.getAvgValue() : 0;

        double std = stats != null && stats.getStdValue() != null ? stats.getStdValue() : 0;

        // 고객 요약 조회
        Page<CustomerSummaryProjection> result = summaryRepository.findCustomerSummary(pageable);

        // 매핑
        Page<CustomerSummaryResponse> mapped = result.map(p -> new CustomerSummaryResponse(
                p.getMemberId(),
                p.getName(),
                maskEmail(p.getEmail()),
                maskPhone(p.getPhone()),
                p.getProductName(),
                p.getCreatedAt().toLocalDate() + " ~ 현재",
                p.getTopConsultCategory(),
                calculateFrequency(p.getLast30dConsultCount(), avg, std),
                convertVipType(p.getVipType())
        ));

        return CommonResponse.success(
                PageResponse.from(mapped),
                "고객 목록 조회");
    }

    // Z-score 기반 상담 빈도 계산 (통걔 로직)
    private String calculateFrequency(Double count, double avg, double std) {

        if (count == null || std == 0) {
            return "LOW";
        }

        double z = (count - avg) / std;

        if (z >= 1)
            return "HIGH";
        if (z <= -1)
            return "LOW";

        return "MEDIUM";
    }

    // VIP 타입 변환 (UI 로직)
    private String convertVipType(String type) {

        if (type == null)
            return "일반 고객";

        return switch (type) {
            case "VIP" -> "VIP";
            case "LOYAL" -> "잠재 VIP";
            case "COMMON" -> "일반 고객";
            case "RISK", "SLEEP" -> "이탈 우려 고객";
            case "LOST" -> "이탈 고객";
            default -> "일반 고객";
        };
    }

    // 전화번호 마스킹
    private String maskPhone(String phone) {

        if (phone == null) return null;

        String digits = phone.replaceAll("\\D", "");

        if (digits.length() != 11) return phone;

        return digits.substring(0,3) + "-****-" + digits.substring(7);
    }

    // 이메일 마스킹
    private String maskEmail(String email) {

        if (email == null || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@");
        String id = parts[0];
        String domain = parts[1];

        if (id.length() <= 4) {
            return id.charAt(0) + "**@" + domain;
        }

        String prefix = id.substring(0, 2);
        String suffix = id.substring(id.length() - 2);

        return prefix + "**" + suffix + "@" + domain;
    }

    // 고객 개인 상담 이력 조회
    @Override
    public PageResponse<CustomerConsultDto> getCustomerConsults(Long memberId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<CustomerConsultView> result = adviceRepository.findConsults(memberId, pageable);

        Page<CustomerConsultDto> dtoPage = result.map(v ->
                new CustomerConsultDto(
                        v.getAdviceId(),
                        v.getCategoryName(),
                        v.getDirection(),
                        v.getChannel(),
                        v.getSatisfactionScore(),
                        v.getIsConverted(),
                        v.getCreatedAt()
                )
        );

        return PageResponse.from(dtoPage);
    }

    // 고객 필터링
    @Override
    public CommonResponse<PageResponse<CustomerFilterResponse>> getFilteredCustomers(CustomerFilterRequest request,
            Pageable pageable) {
        Page<CustomerFilterResponse> result = memberRepository.findFilteredCustomers(request, pageable);
        return CommonResponse.success(PageResponse.from(result), "필터링된 고객 목록 조회");
    }

    // 프론트 필드명 매핑
    private String sortField(String frontField){
        return switch (frontField){
            case "name" -> "name"; // 이름
            case "phone" -> "phone"; // 휴대폰 번호
            case "email" -> "email"; // 이메일
            case "frequency" -> "last30dConsultCount"; // 상담빈도
            case "period" -> "createdAt"; // 이용기간
            case "customerType" -> "vipType"; // 고객 분류
            default -> frontField;
        };
    }
}