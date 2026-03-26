package org.backend.domain.batch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "feature_monetary", uniqueConstraints = {
        @UniqueConstraint(name = "uk_monetary_member_date", columnNames = { "member_id", "feature_base_date" })
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Monetary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "monetary_id")
    private Long monetaryId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "feature_base_date")
    private LocalDate featureBaseDate;

    @Column(name = "total_revenue", nullable = false)
    private Long totalRevenue;

    @Column(name = "last_payment_amount", nullable = false)
    private Long lastPaymentAmount;

    @Column(name = "avg_monthly_bill", nullable = false)
    private Float avgMonthlyBill;

    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;

    @Column(name = "payment_count_6m", nullable = false)
    private Integer paymentCount6m;

    @Column(name = "monthly_revenue", nullable = false)
    private Long monthlyRevenue;

    @Column(name = "payment_delay_count", nullable = false)
    private Integer paymentDelayCount;

    @Column(name = "prev_monthly_revenue", nullable = false)
    private Long prevMonthlyRevenue;

    // @Column(name = "is_vip_prev_month", nullable = false)
    // private Boolean vipPrevMonth;
    @Convert(converter = org.hibernate.type.YesNoConverter.class)
    @Column(name = "is_vip_prev_month", nullable = false, length = 1)
    private Boolean vipPrevMonth;

    @Column(name = "avg_order_val", nullable = false)
    private Float avgOrderVal;

    @Column(name = "purchase_cycle", nullable = false)
    private Integer purchaseCycle;
}