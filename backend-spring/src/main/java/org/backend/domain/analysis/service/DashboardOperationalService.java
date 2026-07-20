package org.backend.domain.analysis.service;

import lombok.RequiredArgsConstructor;
import org.backend.domain.analysis.dto.DashboardOperationalSnapshot;
import org.backend.domain.analysis.repository.DashboardRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class DashboardOperationalService {

    private final DashboardRepository dashboardRepository;
    private final Clock clock;

    @Value("${analytics.time-zone:Asia/Seoul}")
    private String businessTimeZone;

    @Cacheable(value = "dashboardOperationalCache", key = "'current'", sync = true)
    public DashboardOperationalSnapshot getCurrent() {
        ZoneId zone = ZoneId.of(businessTimeZone);
        LocalDate businessDate = LocalDate.now(clock.withZone(zone));
        LocalDateTime startUtc = LocalDateTime.ofInstant(
                businessDate.atStartOfDay(zone).toInstant(), ZoneOffset.UTC);
        LocalDateTime endUtc = LocalDateTime.ofInstant(
                businessDate.plusDays(1).atStartOfDay(zone).toInstant(), ZoneOffset.UTC);
        LocalDateTime nowUtc = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);

        return new DashboardOperationalSnapshot(
                dashboardRepository.countCurrentCustomers(nowUtc.toString()),
                dashboardRepository.countNewCustomers(startUtc.toString(), endUtc.toString()),
                dashboardRepository.countChurnedCustomersByStatusChange(
                        startUtc.toString(), endUtc.toString()),
                nowUtc
        );
    }
}
