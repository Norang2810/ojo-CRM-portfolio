package org.backend.domain.batch.repository;

import org.backend.domain.batch.entity.FeatureUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeatureUsageRepository extends JpaRepository<FeatureUsage, Long> {
    // 특정 회원의 가장 최근 배치 기준일 데이터 조회
    Optional<FeatureUsage> findFirstByMemberIdOrderByFeatureBaseDateDesc(Long memberId);
}