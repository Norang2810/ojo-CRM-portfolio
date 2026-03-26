package org.backend.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.backend.domain.analysis.entity.Invoice;
import org.backend.domain.analysis.entity.Payment;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class MonthlyBillingResponse {

    private String baseMonth;
    private Long billedAmount;
    private LocalDate dueDate;
    private String billingStatus;
    private List<ServiceItemResponse> services;

    public static MonthlyBillingResponse from(Invoice invoice) {

        String status = calculateStatus(invoice);

        return new MonthlyBillingResponse(
                invoice.getBaseMonth(),
                invoice.getBilledAmount(),
                invoice.getDueDate(),
                status,
                invoice.getInvoiceDetails().stream()
                        .map(ServiceItemResponse::from)
                        .toList()
        );
    }

    // 청구서 결제 상태 계산
    // 1. 결제 내역이 없으면 → UNPAID
    // 2. 총 결제 금액이 청구 금액 이상이면 → PAID
    // 3. 일부만 결제된 경우 → PARTIAL
    private static String calculateStatus(Invoice invoice) {

        // 결제 내역 x -> 미납 처리
        if (invoice.getPayments() == null || invoice.getPayments().isEmpty()) {
            return "UNPAID";
        }

        // 모든 결제 금액 합산
        Long totalPaid = invoice.getPayments().stream()
                .map(Payment::getPaidAmount)
                .reduce(0L, Long::sum);

        // 총 결제 금액이 청구 금액 이상이면 완납
        if (totalPaid >= invoice.getBilledAmount()) {
            return "PAID";
        }

        // 일부만 납부된 경우
        return "PARTIAL";
    }
}
