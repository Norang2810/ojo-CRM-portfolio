package org.backend.domain.analysis.repository;

import org.backend.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface DashboardRepository extends JpaRepository<Member, Long> {

    // 1. Current Total Customers (Status != TERMINATED)
    @Query(value = "SELECT COUNT(*) FROM member WHERE status != 'TERMINATED' AND created_at <= :endDate", nativeQuery = true)
    long countCurrentCustomers(@Param("endDate") String endDate);

    // 2. New Customers (created between startDate and endDate)
    @Query(value = "SELECT COUNT(*) FROM member WHERE created_at >= :startDate AND created_at <= :endDate", nativeQuery = true)
    long countNewCustomers(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // 3. New Active Customers 
    // New customers this month who have usage > 0 or whatever. Assuming new + active status.
    @Query(value = """
        SELECT COUNT(DISTINCT fl.member_id)
        FROM feature_lifecycle fl
        JOIN feature_lifecycle prev_fl 
          ON fl.member_id = prev_fl.member_id 
          AND prev_fl.feature_base_date = DATE_SUB(fl.feature_base_date, INTERVAL 1 DAY)
        WHERE fl.feature_base_date >= :startDate AND fl.feature_base_date <= :endDate
          AND prev_fl.is_dormant_flag = 'Y'
          AND fl.is_dormant_flag = 'N'
        """, nativeQuery = true)
    long countNewActiveCustomers(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // 4. At Risk Customers (from analysis table)
    @Query(value = "SELECT COUNT(*) FROM analysis a JOIN (SELECT member_id, MAX(created_at) as max_date FROM analysis GROUP BY member_id) latest ON a.member_id = latest.member_id AND a.created_at = latest.max_date WHERE a.type IN ('RISK', 'SLEEP') AND a.created_at >= :startDate AND a.created_at <= :endDate", nativeQuery = true)
    long countAtRiskCustomers(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // 5. Daily Stats for Last 7 Days
    @Query(value = """
        SELECT DATE(created_at) as statDate, 
               COUNT(*) as newCount
        FROM member 
        WHERE created_at >= :startDate AND created_at <= :endDate
        GROUP BY DATE(created_at)
        """, nativeQuery = true)
    List<Map<String, Object>> getDailyNewCustomers(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Query(value = """
        SELECT DATE(created_at) as statDate, 
               COUNT(*) as churnedCount
        FROM member 
        WHERE status = 'TERMINATED' AND created_at >= :startDate AND created_at <= :endDate
        GROUP BY DATE(created_at)
        """, nativeQuery = true)
    List<Map<String, Object>> getDailyChurnedCustomers(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // Active customers by date - Count of members who are not dormant on that date.
    @Query(value = """
        SELECT DATE(feature_base_date) as statDate, 
               COUNT(DISTINCT member_id) as activeCount
        FROM feature_lifecycle 
        WHERE feature_base_date >= :startDate AND feature_base_date <= :endDate AND is_dormant_flag = 'N'
        GROUP BY DATE(feature_base_date)
        """, nativeQuery = true)
    List<Map<String, Object>> getDailyActiveCustomers(@Param("startDate") String startDate, @Param("endDate") String endDate);

    // 6. Segments Breakdowns (latest analysis per member)
    @Query(value = """
        SELECT a.type, COUNT(*) as cnt
        FROM analysis a
        JOIN (SELECT member_id, MAX(created_at) as max_date FROM analysis GROUP BY member_id) latest
          ON a.member_id = latest.member_id AND a.created_at = latest.max_date
        GROUP BY a.type
        """, nativeQuery = true)
    List<Map<String, Object>> getSegmentCounts();
}
