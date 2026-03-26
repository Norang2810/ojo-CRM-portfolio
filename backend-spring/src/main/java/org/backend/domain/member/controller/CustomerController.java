package org.backend.domain.member.controller;

import lombok.RequiredArgsConstructor;
import org.backend.common.CommonResponse;
import org.backend.common.page.PageResponse;
import org.backend.domain.member.dto.*;
import org.backend.domain.member.service.CustomerService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

        private final CustomerService customerService;

        // 고객별 기본 정보 조회
        // 로그인 구현 후 수정예정
        @GetMapping("/{memberId}")
        public ResponseEntity<CommonResponse<CustomerDetailResponse>> getCustomerDetail(
                        @PathVariable Long memberId) {
                return ResponseEntity.ok(
                                customerService.getCustomerDetail(memberId));
        }

        // 고객 전체 리스트
        @PostMapping("/list")
        public ResponseEntity<CommonResponse<PageResponse<CustomerSummaryResponse>>> getCustomerList(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "10") int size,
                @RequestBody(required = false)List<MemberSortRequest> sorts) {
                return ResponseEntity.ok(
                                customerService.getCustomerList(page, size, sorts));
        }

        // 고객 개인 상담 이력 조회
        @GetMapping("/consults")
        public CommonResponse<PageResponse<CustomerConsultDto>> getCustomerConsults(
                @RequestParam Long memberId,
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "10") int size
        ) {
            PageResponse<CustomerConsultDto> response =
                    customerService.getCustomerConsults(memberId, page, size);
            return CommonResponse.success(response, "고객 상담 이력 조회");
        }


        // 고객 필터링 API
        @PostMapping("/filter")
        public ResponseEntity<CommonResponse<PageResponse<CustomerFilterResponse>>> filterCustomers(
                        @RequestBody CustomerFilterRequest request,
                        Pageable pageable) {
                return ResponseEntity.ok(
                                customerService.getFilteredCustomers(request, pageable));
        }
}
