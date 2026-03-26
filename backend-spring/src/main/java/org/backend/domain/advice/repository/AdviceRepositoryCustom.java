package org.backend.domain.advice.repository;

import org.backend.domain.advice.dto.HourlyConsultationDto;
import java.util.List;

public interface AdviceRepositoryCustom {
    List<HourlyConsultationDto> getHourlyStatistics();
}
