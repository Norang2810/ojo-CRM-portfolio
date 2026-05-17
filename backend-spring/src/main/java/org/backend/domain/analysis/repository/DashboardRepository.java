package org.backend.domain.analysis.repository;

import org.backend.domain.analysis.dto.projection.DashboardDailyCountProjection;
import org.backend.domain.analysis.dto.projection.DashboardSegmentCountProjection;
import org.backend.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashboardRepository extends JpaRepository<Member, Long> {

    @Query(value = """
        SELECT COUNT(*)
        FROM member
        WHERE created_at < :endDate
          AND status <> 'TERMINATED'
        """, nativeQuery = true)
    long countCurrentCustomers(@Param("endDate") String endDate);

    @Query(value = """
        SELECT COUNT(*)
        FROM member
        WHERE created_at >= :startDate
          AND created_at < :endDate
        """, nativeQuery = true)
    long countNewCustomers(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Query(value = """
        SELECT COUNT(DISTINCT fl.member_id)
        FROM feature_lifecycle fl
        JOIN feature_lifecycle prev_fl
          ON fl.member_id = prev_fl.member_id
         AND prev_fl.feature_base_date = DATE_SUB(fl.feature_base_date, INTERVAL 1 DAY)
        WHERE fl.feature_base_date >= :startDate
          AND fl.feature_base_date < :endDate
          AND prev_fl.is_dormant_flag = 'Y'
          AND fl.is_dormant_flag = 'N'
        """, nativeQuery = true)
    long countNewActiveCustomers(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Query(value = """
        SELECT COUNT(*)
        FROM analysis a
        JOIN (
            SELECT member_id, MAX(created_at) AS max_date
            FROM analysis
            GROUP BY member_id
        ) latest
          ON a.member_id = latest.member_id
         AND a.created_at = latest.max_date
        WHERE a.type IN ('RISK', 'SLEEP')
          AND a.created_at >= :startDate
          AND a.created_at < :endDate
        """, nativeQuery = true)
    long countAtRiskCustomers(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Query(value = """
        SELECT DATE(created_at) AS statDate,
               COUNT(*) AS countValue
        FROM member
        WHERE created_at >= :startDate
          AND created_at < :endDate
        GROUP BY DATE(created_at)
        """, nativeQuery = true)
    List<DashboardDailyCountProjection> getDailyNewCustomers(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Query(value = """
        SELECT DATE(created_at) AS statDate,
               COUNT(*) AS countValue
        FROM member
        WHERE status = 'TERMINATED'
          AND created_at >= :startDate
          AND created_at < :endDate
        GROUP BY DATE(created_at)
        """, nativeQuery = true)
    List<DashboardDailyCountProjection> getDailyChurnedCustomers(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Query(value = """
        SELECT DATE(feature_base_date) AS statDate,
               COUNT(DISTINCT member_id) AS countValue
        FROM feature_lifecycle
        WHERE feature_base_date >= :startDate
          AND feature_base_date < :endDate
          AND is_dormant_flag = 'N'
        GROUP BY DATE(feature_base_date)
        """, nativeQuery = true)
    List<DashboardDailyCountProjection> getDailyActiveCustomers(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Query(value = """
        SELECT a.type AS type,
               COUNT(*) AS countValue
        FROM analysis a
        JOIN (
            SELECT member_id, MAX(created_at) AS max_date
            FROM analysis
            GROUP BY member_id
        ) latest
          ON a.member_id = latest.member_id
         AND a.created_at = latest.max_date
        GROUP BY a.type
        """, nativeQuery = true)
    List<DashboardSegmentCountProjection> getSegmentCounts();
}
