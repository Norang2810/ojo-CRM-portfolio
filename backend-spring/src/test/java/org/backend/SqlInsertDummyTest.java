package org.backend;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class SqlInsertDummyTest {

    @Test
    public void insertDummy() throws Exception {
        String url = "jdbc:mysql://127.0.0.1:3306/ojo?serverTimezone=Asia/Seoul&characterEncoding=UTF-8";
        String user = "root";
        String pass = "test1234";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {

            // Clean previous dummy
            stmt.executeUpdate("DELETE FROM analysis WHERE rfm_score = 999");
            stmt.executeUpdate("DELETE FROM feature_usage WHERE max_usage_amount = 99999");
            stmt.executeUpdate("DELETE FROM member WHERE name LIKE 'DummyTester_API%'");

            // Insert member 
            stmt.executeUpdate("INSERT INTO member (name, phone, email, gender, birth_date, region, address, household_type, created_at, status) " +
                    "VALUES ('DummyTester_API_VIP', '010-1234-5678', 'dummy_api@test.com', 'M', '1990-01-01', 'Seoul', 'Seoul', 1, NOW(), 'ACTIVE')");
            stmt.executeUpdate("INSERT INTO member (name, phone, email, gender, birth_date, region, address, household_type, created_at, status) " +
                    "VALUES ('DummyTester_API_CHURN', '010-1234-5679', 'dummy2@test.com', 'F', '1990-01-01', 'Seoul', 'Seoul', 1, DATE_SUB(NOW(), INTERVAL 3 DAY), 'TERMINATED')");
            
            long maxIdVip = 0, maxIdChurn = 0;
            var rs = stmt.executeQuery("SELECT member_id FROM member WHERE name = 'DummyTester_API_VIP'");
            if(rs.next()) maxIdVip = rs.getLong(1);
            
            rs = stmt.executeQuery("SELECT member_id FROM member WHERE name = 'DummyTester_API_CHURN'");
            if(rs.next()) maxIdChurn = rs.getLong(1);
            
            // Insert analysis
            if (maxIdVip > 0) stmt.executeUpdate("INSERT INTO analysis (member_id, rfm_score, type, ltv, lifecycle_stage, created_at) VALUES (" + maxIdVip + ", 999, 'VIP', 1000, 'Stage', NOW())");
            if (maxIdChurn > 0) stmt.executeUpdate("INSERT INTO analysis (member_id, rfm_score, type, ltv, lifecycle_stage, created_at) VALUES (" + maxIdChurn + ", 999, 'LOST', 100, 'Stage', NOW())");

            // Insert feature usage
            if (maxIdVip > 0) stmt.executeUpdate("INSERT INTO feature_usage (member_id, feature_base_date, total_usage_amount, avg_daily_usage, max_usage_amount, usage_peak_hour, premium_service_count, last_activity_date, usage_active_days_30d) VALUES (" + maxIdVip + ", NOW(), 500, 10, 99999, 12, 1, NOW(), 5)");

            System.out.println("DUMMY DATA INSERTED SUCCESSFULLY");
        }
    }
}
