package org.backend.domain.analysis.repository;

import org.backend.domain.analysis.entity.DashboardSummaryStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DashboardSummaryStatsRepository extends JpaRepository<DashboardSummaryStats, Long> {

    Optional<DashboardSummaryStats> findByStatDate(LocalDate statDate);

    Optional<DashboardSummaryStats> findTopByOrderByStatDateDesc();
}