package org.backend.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.backend.domain.analysis.entity.InvoiceDetail;

@Getter
@AllArgsConstructor
public class ServiceItemResponse {

    private String productName;
    private String productType;
    private Long amount;

    public static ServiceItemResponse from(InvoiceDetail detail) {
        return new ServiceItemResponse(
                detail.getProductNameSnapshot(),
                detail.getProductType(),
                detail.getTotalPrice()
        );
    }
}
