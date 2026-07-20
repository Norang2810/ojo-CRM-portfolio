package org.backend.domain.analysis.dto;

import java.time.LocalDateTime;

public record DashboardOperationalSnapshot(
        long currentCustomers,
        long todayNewCustomers,
        long todayChurnedCustomers,
        LocalDateTime dataAsOf
) {
}
