
package org.backend.domain.subscription.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.backend.domain.member.entity.Member;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "subscription_period")
public class SubscriptionPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_period_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private Long quantity;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "reason_code", columnDefinition = "TEXT")
    private String reasonCode;

}
