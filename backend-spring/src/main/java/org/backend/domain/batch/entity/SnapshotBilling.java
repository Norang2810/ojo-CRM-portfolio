package org.backend.domain.batch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.backend.domain.member.entity.Member;

@Entity
@Table(name = "snapshot_billing")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SnapshotBilling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id")
    private Long Id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "base_month", length = 6, nullable = false)
    private String baseMonth;

    @Column(name = "billed_amount", nullable = false)
    private Long billedAmount;

    @Column(name = "overdue_amount")
    private Long overdueAmount;

    @Column(name = "overdue_months")
    private Long overdueMonths;

    @Column(name = "grade", length=20)
    private String grade;

    @Column(name = "month_revenue", nullable = false)
    private Long monthRevenue;

    @Column(name = "total_revenue")
    private Long totalRevenue;
}
