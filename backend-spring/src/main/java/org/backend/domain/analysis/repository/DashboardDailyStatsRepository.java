package org.backend.domain.analysis.repository;

import org.backend.domain.analysis.entity.DashboardDailyStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DashboardDailyStatsRepository extends JpaRepository<DashboardDailyStats, Long> {

    Optional<DashboardDailyStats> findByStatDate(LocalDate statDate);

    List<DashboardDailyStats> findByStatDateBetweenOrderByStatDateAsc(LocalDate startDate, LocalDate endDate);

    void deleteByStatDateBetween(LocalDate startDate, LocalDate endDate);
}