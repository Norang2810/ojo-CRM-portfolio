package org.backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class SqlInsertDummy {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://127.0.0.1:3306/ojo?serverTimezone=Asia/Seoul&characterEncoding=UTF-8";
        String user = "root";
        String pass = "test1234";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {

            // Clean previous dummy
            stmt.executeUpdate("DELETE FROM analysis WHERE rfm_score = 999");
            stmt.executeUpdate("DELETE FROM feature_usage WHERE max_usage_amount = 99999");
            stmt.executeUpdate("DELETE FROM member WHERE name = 'DummyTester_API'");

            // Insert member 
            stmt.executeUpdate("INSERT INTO member (name, phone, email, gender, birth_date, region, address, household_type, created_at, status) " +
                    "VALUES ('DummyTester_API', '010-1234-5678', 'dummy_api@test.com', 'M', '1990-01-01', 'Seoul', 'Seoul', 1, NOW(), 'ACTIVE')");
            
            long maxId = 0;
            var rs = stmt.executeQuery("SELECT MAX(member_id) FROM member");
            if(rs.next()) maxId = rs.getLong(1);
            
            // Insert analysis
            stmt.executeUpdate("INSERT INTO analysis (member_id, rfm_score, type, ltv, lifecycle_stage, created_at) " +
                    "VALUES (" + maxId + ", 999, 'VIP', 1000, 'Stage', NOW())");

            // Insert feature usage
            stmt.executeUpdate("INSERT INTO feature_usage (member_id, feature_base_date, total_usage_amount, avg_daily_usage, max_usage_amount, usage_peak_hour, premium_service_count, last_activity_date, usage_active_days_30d) " +
                    "VALUES (" + maxId + ", NOW(), 500, 10, 99999, '12:00', 1, NOW(), 5)");

            System.out.println("DUMMY DATA INSERTED SUCCESSFULLY FOR MEMBER ID: " + maxId);
        }
    }
}
