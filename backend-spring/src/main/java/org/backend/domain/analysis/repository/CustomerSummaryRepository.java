package org.backend.domain.analysis.repository;

import org.backend.domain.member.dto.CustomerSummaryProjection;
import org.backend.domain.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerSummaryRepository extends JpaRepository<Member, Long> {

    /**
     * 조회 항목 설명:
     *
     * 1. memberId
     *    - 기준: member.member_id
     *    - 고객 PK
     *
     * 2. name
     *    - 기준: member.name
     *    - 고객 이름
     *
     * 3. createdAt
     *    - 기준: member.created_at
     *    - 고객 가입일 (이용기간 계산용)
     *
     * 4. topConsultCategory
     *    - 기준: feature_consultation.top_consult_category
     *    - 최근 상담 상위 카테고리
     *
     * 5. last30dConsultCount
     *    - 기준: feature_consultation.last_30d_consult_count
     *    - 최근 30일 상담 건수 (Z-score 계산용)
     *
     * 6. vipType
     *    - 기준: analysis.type
     *    - 고객 세그먼트 분류값 (VIP, LOYAL, COMMON, RISK, LOST 등)
     *
     * 7. productName
     *    - 기준: subscription_period + product
     *    - 조건: status = 'ACTIVE'
     *    - 정렬: started_at DESC
     *    - 가장 최근 사용 중인 서비스 1개 조회
     */
    @Query(value = """
        SELECT
            m.member_id AS memberId,
            m.name AS name,
            m.email AS email,
            m.phone AS phone,
            m.created_at AS createdAt,
            fc.top_consult_category AS topConsultCategory,
            fc.last_30d_consult_count AS last30dConsultCount,
            a.type AS vipType,
            (
                SELECT p.product_name
                FROM subscription_period sp
                JOIN product p ON sp.product_id = p.product_id
                WHERE sp.member_id = m.member_id
                  AND sp.status = 'ACTIVE'
                ORDER BY sp.started_at DESC
                LIMIT 1
            ) AS productName
        FROM member m
        LEFT JOIN (
            SELECT fc1.member_id, fc1.top_consult_category, fc1.last_30d_consult_count
            FROM feature_consultation fc1
            INNER JOIN (
                SELECT member_id, MAX(feature_base_date) as max_date
                FROM feature_consultation
                GROUP BY member_id
            ) fc2 ON fc1.member_id = fc2.member_id AND fc1.feature_base_date = fc2.max_date
        ) fc ON m.member_id = fc.member_id
        LEFT JOIN (
            SELECT a1.member_id, a1.type
            FROM analysis a1
            INNER JOIN (
                SELECT member_id, MAX(created_at) as max_date
                FROM analysis
                GROUP BY member_id
            ) a2 ON a1.member_id = a2.member_id AND a1.created_at = a2.max_date
        ) a ON m.member_id = a.member_id
        WHERE m.status != 'TERMINATED'
          AND m.created_at IS NOT NULL
          AND m.created_at <= NOW()
        """,
            countQuery = "SELECT COUNT(*) FROM member WHERE status != 'TERMINATED' AND created_at IS NOT NULL AND created_at <= NOW()",
            nativeQuery = true)
    Page<CustomerSummaryProjection> findCustomerSummary(Pageable pageable);

    //고객 검색
    @Query(value = """
    SELECT
        m.member_id AS memberId,
        m.name AS name,
        m.email AS email,
        m.phone AS phone,
        m.created_at AS createdAt,
        fc.top_consult_category AS topConsultCategory,
        fc.last_30d_consult_count AS last30dConsultCount,
        a.type AS vipType,
        (
            SELECT p.product_name
            FROM subscription_period sp
            JOIN product p ON sp.product_id = p.product_id
            WHERE sp.member_id = m.member_id
              AND sp.status = 'ACTIVE'
            ORDER BY sp.started_at DESC
            LIMIT 1
        ) AS productName
    FROM member m
    LEFT JOIN (
        SELECT fc1.member_id, fc1.top_consult_category, fc1.last_30d_consult_count
        FROM feature_consultation fc1
        INNER JOIN (
            SELECT member_id, MAX(feature_base_date) as max_date
            FROM feature_consultation
            WHERE member_id IN (:memberIds)
            GROUP BY member_id
        ) fc2 ON fc1.member_id = fc2.member_id AND fc1.feature_base_date = fc2.max_date
    ) fc ON m.member_id = fc.member_id
    LEFT JOIN (
        SELECT a1.member_id, a1.type
        FROM analysis a1
        INNER JOIN (
            SELECT member_id, MAX(created_at) as max_date
            FROM analysis
            WHERE member_id IN (:memberIds)
            GROUP BY member_id
        ) a2 ON a1.member_id = a2.member_id AND a1.created_at = a2.max_date
    ) a ON m.member_id = a.member_id
    WHERE m.member_id IN (:memberIds)
      AND m.created_at IS NOT NULL
      AND m.created_at <= NOW()
    """,
            nativeQuery = true)
    List<CustomerSummaryProjection> findCustomerSummaryByMemberIds(
            @Param("memberIds") List<Long> memberIds
    );
}
