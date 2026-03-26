package org.backend.domain.analysis.service;

import org.backend.common.CommonResponse;
import org.backend.domain.analysis.dto.MonthlyBillingResponse;

public interface BillingService {

    CommonResponse<MonthlyBillingResponse> getMonthlyBilling(String baseMonth, Long memberId);
}