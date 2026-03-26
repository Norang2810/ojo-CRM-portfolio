package org.backend.domain.analysis.entity;


import jakarta.persistence.*;
import lombok.Getter;
import org.backend.domain.subscription.entity.Product;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_detail")
@Getter
public class InvoiceDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_detail_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_name_snapshot", nullable = false, length = 50)
    private String productNameSnapshot;

    @Column(name = "product_type", nullable = false, length = 20)
    private String productType;

    @Column
    private Long quantity;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

}