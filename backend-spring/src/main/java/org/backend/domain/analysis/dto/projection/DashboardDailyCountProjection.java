package org.backend.domain.analysis.dto.projection;

import java.time.LocalDate;

public interface DashboardDailyCountProjection {
    LocalDate getStatDate();
    Long getCountValue();
}
