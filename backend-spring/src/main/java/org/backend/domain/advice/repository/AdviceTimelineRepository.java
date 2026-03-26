package org.backend.domain.advice.repository;

import org.backend.domain.advice.entity.Advice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdviceTimelineRepository extends JpaRepository<Advice, Long> {

    @EntityGraph(attributePaths = {"category", "promotion"})
    List<Advice> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}