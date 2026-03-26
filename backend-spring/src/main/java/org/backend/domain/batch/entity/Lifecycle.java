package org.backend.domain.batch.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.type.YesNoConverter; // 임포트 추가
import java.time.LocalDate;

@Entity
@Table(name = "feature_lifecycle", uniqueConstraints = {
        @UniqueConstraint(name = "uk_lifecycle_member_date", columnNames = { "member_id", "feature_base_date" })
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lifecycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lifecycle_id")
    private Long lifecycleId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "feature_base_date")
    private LocalDate featureBaseDate;

    @Column(name = "member_lifetime_days", nullable = false)
    private Integer memberLifetimeDays;

    @Column(name = "days_since_last_activity")
    private Integer daysSinceLastActivity;

    @Column(name = "contract_end_days_left")
    private Integer contractEndDaysLeft;

    // Y/N 컨버터 적용 및 길이 1로 고정
    @Convert(converter = YesNoConverter.class)
    @Column(name = "is_dormant_flag", nullable = false, length = 1)
    private Boolean isDormantFlag;

    @Convert(converter = YesNoConverter.class)
    @Column(name = "is_new_customer_flag", nullable = false, length = 1)
    private Boolean isNewCustomerFlag;

    @Convert(converter = YesNoConverter.class)
    @Column(name = "is_terminated_flag", nullable = false, length = 1)
    private Boolean isTerminatedFlag;

    @Column(name = "signup_date", nullable = false)
    private LocalDate signupDate;
}