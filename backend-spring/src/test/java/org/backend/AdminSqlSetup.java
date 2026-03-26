package org.backend;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import org.junit.jupiter.api.Test;

public class AdminSqlSetup {
    @Test
    public void setupAdmin() throws Exception {
        String url = "jdbc:mysql://127.0.0.1:3306/ojo?serverTimezone=Asia/Seoul&characterEncoding=UTF-8";
        String user = "root";
        String pass = "test1234";

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode("1234");

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            // Check if admin exists
            var rs = conn.createStatement().executeQuery("SELECT admin_id FROM admin WHERE email = 'admin@test.com'");
            if (rs.next()) {
                System.out.println("Admin already exists. Updating password...");
                try (PreparedStatement pstmt = conn.prepareStatement("UPDATE admin SET password = ? WHERE email = 'admin@test.com'")) {
                    pstmt.setString(1, hashedPassword);
                    pstmt.executeUpdate();
                }
            } else {
                System.out.println("Inserting new admin...");
                String sql = "INSERT INTO admin (name, email, password, phone, google, role, status, created_at, updated_at) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, "관리자");
                    pstmt.setString(2, "admin@test.com");
                    pstmt.setString(3, hashedPassword);
                    pstmt.setString(4, "010-0000-0000");
                    pstmt.setBoolean(5, false);
                    pstmt.setString(6, "ADMIN");
                    pstmt.setString(7, "ACTIVE");
                    pstmt.executeUpdate();
                }
            }
            System.out.println("========== ADMIN SETUP COMPLETE ==========");
            System.out.println("Email: admin@test.com");
            System.out.println("Password: 1234");
            System.out.println("==========================================");
        }
    }
}
