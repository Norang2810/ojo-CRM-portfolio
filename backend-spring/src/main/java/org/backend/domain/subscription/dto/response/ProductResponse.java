package org.backend.domain.subscription.dto.response;

import lombok.Getter;

@Getter
public class ProductResponse {

    private final Long planId;
    private final String productName;
    private final String productType;
    private final Long price;

    public ProductResponse(Long planId, String productName, String productType, Long price) {
        this.planId = planId;
        this.productName = productName;
        this.productType = productType;
        this.price = price;
    }

}
