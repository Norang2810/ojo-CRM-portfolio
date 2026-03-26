package org.backend.domain.analysis.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "rfm_kpi")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RfmKpi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kpi_id")
    private Long id;

    @Column(precision = 5, scale = 4)
    private BigDecimal crr;

    @Column(name = "churn_rate", precision = 5, scale = 4)
    private BigDecimal churnRate;

    @Column(precision = 5, scale = 4)
    private BigDecimal nrr;

    @Column(name = "base_month", length = 7, nullable = false)
    private String baseMonth;
}
