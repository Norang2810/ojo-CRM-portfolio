package org.backend.domain.batch.repository;

import org.backend.domain.batch.entity.Monetary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonetaryRepository extends JpaRepository<Monetary, Long> {
    Optional<Monetary> findFirstByMemberIdOrderByFeatureBaseDateDesc(Long memberId);
}