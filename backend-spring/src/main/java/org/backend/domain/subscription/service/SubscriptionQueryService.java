package org.backend.domain.subscription.service;

import org.backend.domain.subscription.dto.response.SubscriptionListResponse;

public interface SubscriptionQueryService {

    SubscriptionListResponse getActiveSubscriptions(Long memberId);
}
