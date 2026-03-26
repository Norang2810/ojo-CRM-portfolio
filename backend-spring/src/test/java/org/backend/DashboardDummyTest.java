package org.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.backend.domain.analysis.dto.DashboardSummaryResponseDto;
import org.backend.domain.analysis.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.elasticsearch.repositories.enabled=false",
    "spring.elasticsearch.uris=http://localhost:9999" // Dummy url so it doesn't fail immediately inside client creation? No, we just disable it.
})
public class DashboardDummyTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testWithDummyData() throws Exception {
        // Insert dummy member
        try {
            jdbcTemplate.execute("DELETE FROM analysis WHERE rfm_score = 999");
            jdbcTemplate.execute("DELETE FROM member WHERE name = 'DummyTester123'");
        } catch(Exception e) {}

        jdbcTemplate.execute("INSERT INTO member (name, phone, email, gender, birth_date, region, address, household_type, created_at, status) " +
                "VALUES ('DummyTester123', '010-0000-0000', 'dummy@test.com', 'M', '1990-01-01', 'Seoul', 'Seoul', 1, NOW(), 'ACTIVE')");
        
        Long memberId = jdbcTemplate.queryForObject("SELECT member_id FROM member WHERE name = 'DummyTester123' LIMIT 1", Long.class);

        jdbcTemplate.execute("INSERT INTO analysis (member_id, rfm_score, type, ltv, lifecycle_stage, created_at) " +
                "VALUES (" + memberId + ", 999, 'VIP', 1000, 'Stage', NOW())");

        DashboardSummaryResponseDto result = dashboardService.getDashboardSummary();
        System.out.println("========== DUMMY TEST RESULT ==========");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
        System.out.println("=======================================");
        
        // Clean up
        jdbcTemplate.execute("DELETE FROM analysis WHERE rfm_score = 999");
        jdbcTemplate.execute("DELETE FROM member WHERE name = 'DummyTester123'");
    }
}
