package org.backend.domain.subscription.repository;

import org.backend.domain.subscription.entity.SubscriptionPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionPeriodRepository extends JpaRepository<SubscriptionPeriod, Long> {

    List<SubscriptionPeriod> findAllByMember_IdAndStatus(Long memberId, String status);
}
