package org.backend.domain.member.service;

import org.backend.common.CommonResponse;
import org.backend.common.page.PageResponse;
import org.backend.domain.member.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomerService {

    // 고객별 기본 정보 조회
    CommonResponse<CustomerDetailResponse> getCustomerDetail(Long memberId);

    // 고객 전체 리스트
    CommonResponse<PageResponse<CustomerSummaryResponse>> getCustomerList(int page, int size);
    CommonResponse<PageResponse<CustomerSummaryResponse>> getCustomerList(int page, int size, List<MemberSortRequest> sorts);

    // 고객 상담 이력 조회
    PageResponse<CustomerConsultDto> getCustomerConsults(Long memberId, int page, int size);

    // 고객 필터링
    CommonResponse<PageResponse<CustomerFilterResponse>> getFilteredCustomers(CustomerFilterRequest request,
            Pageable pageable);
}
