package org.backend.domain.analysis.service;

import lombok.RequiredArgsConstructor;
import org.backend.common.CommonResponse;
import org.backend.common.exception.CustomException;
import org.backend.common.exception.ErrorCode;
import org.backend.domain.analysis.dto.MonthlyBillingResponse;
import org.backend.domain.analysis.entity.Invoice;
import org.backend.domain.analysis.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingServiceImpl implements BillingService {

    private final InvoiceRepository invoiceRepository;

    // 고객별 청구서 조회
    @Override
    public CommonResponse<MonthlyBillingResponse> getMonthlyBilling(String baseMonth, Long memberId) {

        Invoice invoice = invoiceRepository
                .findByMemberIdAndBaseMonth(memberId, baseMonth)
                .orElseThrow(() -> new CustomException(ErrorCode.INVOICE_NOT_FOUND));

        MonthlyBillingResponse response =
                MonthlyBillingResponse.from(invoice);

        return CommonResponse.success(response, "고객 특정 월 이용 요금 조회 성공");
    }
}