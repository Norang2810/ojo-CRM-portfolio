package org.backend.domain.analysis.service;

import lombok.RequiredArgsConstructor;
import org.backend.common.exception.CustomException;
import org.backend.common.exception.ErrorCode;
import org.backend.domain.analysis.dto.DashboardKpiResponseDto;
import org.backend.domain.analysis.dto.RfmKpiResponseDto;
import org.backend.domain.analysis.entity.RfmKpi;
import org.backend.domain.analysis.repository.RfmKpiRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KpiService {
        private final RfmKpiRepository rfmKpiRepository;

        public RfmKpiResponseDto getKpi(String baseMonth) {
                String formattedMonth = (baseMonth != null) ? baseMonth.replace("-", "") : "";

                RfmKpi rfmKpi = rfmKpiRepository.findByBaseMonth(formattedMonth)
                                .orElseThrow(() -> new CustomException(ErrorCode.KPI_NOT_FOUND));
                return new RfmKpiResponseDto(
                                rfmKpi.getBaseMonth(),
                                rfmKpi.getCrr(),
                                evaluateStatus("CRR", rfmKpi.getCrr()),
                                rfmKpi.getChurnRate(),
                                evaluateStatus("CHURN", rfmKpi.getChurnRate()),
                                rfmKpi.getNrr(),
                                evaluateStatus("NRR", rfmKpi.getNrr()));
        }

        public List<DashboardKpiResponseDto> getDashboardKpis() {
                List<RfmKpi> kpis = rfmKpiRepository.findAll();
                kpis.sort((k1, k2) -> k2.getBaseMonth().compareTo(k1.getBaseMonth())); // 최근 달이 위로

                return kpis.stream()
                                .map(k -> new DashboardKpiResponseDto(
                                                k.getChurnRate(),
                                                k.getCrr(),
                                                k.getNrr(),
                                                k.getBaseMonth(),
                                                k.getId()))
                                .collect(Collectors.toList());
        }

        private String evaluateStatus(String kpi, BigDecimal value) {
                if (value == null)
                        return "UNKNOWN";

                return switch (kpi) {
                        case "CRR" -> value.compareTo(new BigDecimal("0.8")) >= 0 ? "HEALTHY"
                                        : (value.compareTo(new BigDecimal("0.6")) >= 0 ? "WARNING" : "DANGER");
                        case "CHURN" -> value.compareTo(new BigDecimal("0.1")) <= 0 ? "HEALTHY"
                                        : (value.compareTo(new BigDecimal("0.2")) <= 0 ? "WARNING" : "DANGER");
                        case "NRR" -> value.compareTo(new BigDecimal("1.1")) >= 0 ? "HEALTHY"
                                        : (value.compareTo(new BigDecimal("1.0")) >= 0 ? "WARNING" : "DANGER");
                        default -> "HEALTHY";
                };
        }
}
