package org.backend.domain.subscription.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class SubscriptionListResponse {

    private final List<SubscriptionResponse> subscriptions;

    public SubscriptionListResponse(List<SubscriptionResponse> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public List<SubscriptionResponse> getSubscriptions() {
        return subscriptions;
    }
}
