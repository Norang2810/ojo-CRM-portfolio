package org.backend.domain.analysis.repository;

import org.backend.domain.analysis.dto.RfmSegmentResponseDto;
import org.backend.domain.analysis.dto.RfmTrendResponseDto;
import org.backend.domain.analysis.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    Optional<Analysis> findFirstByMemberIdOrderByCreatedAtDesc(Long memberId);

    @Query("SELECT a FROM Analysis a JOIN FETCH a.member")
    List<Analysis> findAllWithMember();

    @Query("SELECT MAX(a.createdAt) FROM Analysis a")
    LocalDateTime findLatestCreatedAt();

    // 전체 세그먼트별 RFM 지표 조회
    @Query("""
                SELECT new org.backend.domain.analysis.dto.RfmSegmentResponseDto$SegmentDetail(
                        a.type, COUNT(a), 0.0,
                        AVG(CAST(FUNCTION('DATEDIFF', a.createdAt, r.recency) AS double)),
                        AVG(r.frequency),
                        AVG(r.monetary)
                    )
                FROM Analysis a JOIN Rfm r on a.member.id = r.memberId
                AND FUNCTION('DATE_FORMAT', a.createdAt, '%Y%m') = FUNCTION('DATE_FORMAT', r.updatedAt, '%Y%m')
                WHERE FUNCTION('DATE_FORMAT', a.createdAt, '%Y%m') = :baseMonth
                GROUP BY a.type
            """)
    List<RfmSegmentResponseDto.SegmentDetail> findRfmStatisticsBySegment(@Param("baseMonth") String baseMonth);

    @Query("""
                SELECT
                    FUNCTION('DATE_FORMAT', a.createdAt, '%Y%m')AS baseMonth,
                    a.type AS type,
                    AVG(r.monetary) AS avgMonetary,
                    COUNT(a) AS count
                FROM Analysis a JOIN Rfm r ON a.member.id = r.memberId
                AND FUNCTION('DATE_FORMAT', a.createdAt, '%Y%m') = FUNCTION('DATE_FORMAT', r.updatedAt, '%Y%m')
                WHERE a.createdAt >= :start
                GROUP BY FUNCTION('DATE_FORMAT', a.createdAt, '%Y%m'), a.type
                ORDER BY FUNCTION('DATE_FORMAT', a.createdAt, '%Y%m')
            """)
    List<RfmTrendResponseDto> findRfmTrend(@Param("start") LocalDateTime start);
    // 대시보드용 - 전체 고객 ltv, 등급별 고객수 등
}
