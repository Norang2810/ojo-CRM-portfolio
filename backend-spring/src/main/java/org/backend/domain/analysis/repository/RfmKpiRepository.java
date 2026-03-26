package org.backend.domain.analysis.repository;

import org.backend.domain.analysis.entity.RfmKpi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RfmKpiRepository extends JpaRepository<RfmKpi, Long> {
    Optional<RfmKpi> findByBaseMonth(String baseMonth);
}
