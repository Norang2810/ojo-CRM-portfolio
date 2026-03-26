package org.backend.domain.advice.repository;

import org.backend.domain.advice.entity.Advice;
import org.backend.domain.advice.view.CustomerConsultView;
import org.backend.domain.member.dto.CustomerConsultDto;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AdviceRepository extends JpaRepository<Advice, Long>, AdviceRepositoryCustom {

    // 고객 개인 상담 이력 조회
    @Query("""
        SELECT
            a.id AS adviceId,
            c.categoryName AS categoryName,
            a.direction AS direction,
            a.channel AS channel,
            a.satisfactionScore AS satisfactionScore,
            (a.promotion IS NOT NULL) AS isConverted,
            a.createdAt AS createdAt
        FROM Advice a
        JOIN a.category c
        WHERE a.member.id = :memberId
        ORDER BY a.createdAt DESC
    """)
    Page<CustomerConsultView> findConsults(@Param("memberId") Long memberId, Pageable pageable);


}