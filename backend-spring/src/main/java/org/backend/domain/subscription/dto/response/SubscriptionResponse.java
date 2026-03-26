package org.backend.domain.subscription.dto.response;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SubscriptionResponse {

    private final Long subscribeId;
    private final ProductResponse product;
    private final Long quantity;
    private final Long totalPrice;
    private final LocalDateTime startedAt;
    private final String status;

    public SubscriptionResponse(Long subscribeId,
                                ProductResponse product,
                                Long quantity,
                                Long totalPrice,
                                LocalDateTime startedAt,
                                String status) {
        this.subscribeId = subscribeId;
        this.product = product;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.startedAt = startedAt;
        this.status = status;
    }

}
