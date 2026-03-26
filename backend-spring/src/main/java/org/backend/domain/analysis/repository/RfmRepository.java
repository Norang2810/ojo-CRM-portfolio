package org.backend.domain.analysis.repository;

import org.backend.domain.analysis.entity.Rfm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RfmRepository extends JpaRepository<Rfm, Long> {
    Optional<Rfm> findFirstByMemberIdOrderByUpdatedAtDesc(Long memberId);
}
