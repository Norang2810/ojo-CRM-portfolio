package org.backend.domain.subscription.controller;

import lombok.RequiredArgsConstructor;
import org.backend.common.CommonResponse;
import org.backend.domain.subscription.dto.response.SubscriptionListResponse;
import org.backend.domain.subscription.service.SubscriptionQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
public class SubscriptionQueryController {

    private final SubscriptionQueryService subscriptionQueryService;

    // 현재 이용 중인 서비스 조회
    @GetMapping("/{memberId}/subscriptions")
    public CommonResponse<SubscriptionListResponse> getActiveSubscriptions(
            // security 적용 후 memberId 제거
            @PathVariable Long memberId
    ) {
        SubscriptionListResponse response = subscriptionQueryService.getActiveSubscriptions(memberId);
        return CommonResponse.success(response, "현재 이용 중인 서비스 조회");
    }
}
