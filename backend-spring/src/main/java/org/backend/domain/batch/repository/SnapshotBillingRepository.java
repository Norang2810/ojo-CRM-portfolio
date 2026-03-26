package org.backend.domain.batch.repository;

import org.backend.domain.batch.entity.SnapshotBilling;
import org.backend.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SnapshotBillingRepository extends JpaRepository<SnapshotBilling, Long> {
    Optional<SnapshotBilling> findTopByMemberOrderByBaseMonthDesc(Member member);
}
