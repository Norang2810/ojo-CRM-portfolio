package org.backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDbTables2 {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3307/ojo", "root", "test1234");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW TABLES");
            while(rs.next()) {
                String table = rs.getString(1);
                Statement countStmt = conn.createStatement();
                ResultSet countRs = countStmt.executeQuery("SELECT COUNT(*) FROM " + table);
                countRs.next();
                System.out.println("Table: " + table + " -> rows: " + countRs.getInt(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
