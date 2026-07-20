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
        FROM analysis_current a
        WHERE a.type IN ('RISK', 'SLEEP')
          AND :startDate <= :endDate
        """, nativeQuery = true)
    long countAtRiskCustomers(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Query(value = """
        SELECT DATE(CONVERT_TZ(created_at, '+00:00', '+09:00')) AS statDate,
               COUNT(*) AS countValue
        FROM member
        WHERE created_at >= :startDate
          AND created_at < :endDate
        GROUP BY DATE(CONVERT_TZ(created_at, '+00:00', '+09:00'))
        """, nativeQuery = true)
    List<DashboardDailyCountProjection> getDailyNewCustomers(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Query(value = """
        SELECT DATE(CONVERT_TZ(changed_at, '+00:00', '+09:00')) AS statDate,
               COUNT(*) AS countValue
        FROM member_status_history
        WHERE status = 'TERMINATED'
          AND changed_at >= :startDate
          AND changed_at < :endDate
        GROUP BY DATE(CONVERT_TZ(changed_at, '+00:00', '+09:00'))
        """, nativeQuery = true)
    List<DashboardDailyCountProjection> getDailyChurnedCustomers(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Query(value = """
        SELECT COUNT(DISTINCT member_id)
        FROM member_status_history
        WHERE status = 'TERMINATED'
          AND changed_at >= :startDate
          AND changed_at < :endDate
        """, nativeQuery = true)
    long countChurnedCustomersByStatusChange(
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

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
        FROM analysis_current a
        GROUP BY a.type
        """, nativeQuery = true)
    List<DashboardSegmentCountProjection> getSegmentCounts();
}
