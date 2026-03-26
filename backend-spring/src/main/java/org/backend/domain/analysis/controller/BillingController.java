package org.backend.domain.analysis.controller;

import lombok.RequiredArgsConstructor;
import org.backend.common.CommonResponse;
import org.backend.domain.analysis.dto.MonthlyBillingResponse;
import org.backend.domain.analysis.service.BillingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    // 고객별 청구서 조회
    // 로그인 구현 후 수정예정
    @GetMapping("/monthly/{baseMonth}/{memberId}")
    public ResponseEntity<CommonResponse<MonthlyBillingResponse>> getMonthlyBilling(
            @PathVariable String baseMonth,
            @PathVariable Long memberId
    ) {
        return ResponseEntity.ok(
                billingService.getMonthlyBilling(baseMonth, memberId)
        );
    }
}
